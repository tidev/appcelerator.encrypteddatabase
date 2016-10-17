/**
 * Eyncrypted Database
 * Copyright (c) 2015-Present by Appcelerator, Inc.
 * All Rights Reserved.
 */

'use strict';

exports.id = 'appcelerator.encrypteddatabase';
exports.cliVersion = '>=3.2';
exports.init = init;

/**
 * main entry point for our plugin which looks for the platform specific
 * plugin to invoke
 */
function init(logger, config, cli, appc) {
	cli.on('build.ios.xcodeproject', {
		pre: function(data) {
			logger.info('Rearranging sqlite3.dylib for proper SQLCipher usage ...');
						
			var PBXNativeTarget = null;
			var PBXNativeTargetUUID = null;
			var buildPhases = null;
			var files = null;
			var targets = null;

			var appName = this.tiapp.name;
			var sqliteLibrary = 'libsqlite3.dylib';
			var hash = data.args[0].hash;
			var objects = hash.project.objects;

			var PBXProject = objects['PBXProject'];
			var PBXFrameworksBuildPhase = objects['PBXFrameworksBuildPhase'];
			var PBXProjectUUID = PBXProject[hash.project.rootObject];
						
			// Get the targets by using the project UUID
			targets = objects['PBXProject'][PBXProjectUUID]['targets'];
			
			// Loop all targets to find the target we need
			for (var i = 0; i < targets.length; i++) {
				if (targets[i].comment == '"' + appName + '"') {
					PBXNativeTarget = objects['PBXNativeTarget'][targets[i].value];
					break;
				}
			}

			// Get the build phases related to this target
			buildPhases = PBXNativeTarget['buildPhases'];
	
			// Get the UUID of the target
			for (var i = 0; i < buildPhases.length; i++) {
				if (buildPhases[i].comment == 'Frameworks') {
					PBXNativeTargetUUID = buildPhases[i].value;
					break;
				}
			}
									
			// Assign the target UUID to get the frameworks of the target
			files = PBXFrameworksBuildPhase[PBXNativeTargetUUID]['files'];
						
			for (var i = 0; i <  files.length; i++) {
				var obj = files[i];
				
				// Find the affected object and only replace it when
				// it's not already the last one (recrurring builds)
				if (obj.comment == sqliteLibrary + ' in Frameworks' && i != files.length - 1) {
					// Remove it from it's initial position
					files.splice(i, 1);
					
					// Insert it as the last element again
					files.push(obj);
					break;
				}
			}
			
			// Re-assign the re-arranged list of frameworks
			data.args[0].hash.project.objects.PBXFrameworksBuildPhase.PBXNativeTargetUUID.files = files;			
		}
	});
}
