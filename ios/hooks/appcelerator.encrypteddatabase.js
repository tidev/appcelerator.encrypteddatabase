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
			logger.info('Rearranging sqlite3.dylib for proper SQLCipher usage...');
						
			let PBXNativeTarget = null;
			let PBXNativeTargetUUID = null;
			let buildPhases = null;
			let files = null;
			let targets = null;

			const appName = this.tiapp.name;
			const sqliteLibrary = 'libsqlite3.dylib';
			const hash = data.args[0].hash;
			const objects = hash.project.objects;

			const PBXProject = objects['PBXProject'];
			const PBXFrameworksBuildPhase = objects['PBXFrameworksBuildPhase'];
			const PBXProjectUUID = PBXProject[hash.project.rootObject];
						
			// Get the targets by using the project UUID
			targets = PBXProjectUUID['targets'];
			
			// Loop all targets to find the target we need
			for (let i = 0; i < targets.length; i++) {
				if (targets[i].comment == '"' + appName + '"') {
					PBXNativeTarget = objects['PBXNativeTarget'][targets[i].value];
					break;
				}
			}
			
			// Get the build phases related to this target
			buildPhases = PBXNativeTarget['buildPhases'];
				
			// Get the UUID of the target
			for (let i = 0; i < buildPhases.length; i++) {
				if (buildPhases[i].comment == 'Frameworks') {
					PBXNativeTargetUUID = buildPhases[i].value;
					break;
				}
			}
												
			// Assign the target UUID to get the frameworks of the target
			files = PBXFrameworksBuildPhase[PBXNativeTargetUUID]['files'];

			let sqliteObj = null; // 'sqlite3.dylib' entry
			let moduleIndex = null; // index of 'libappcelerator.encrypteddatabase.a'

			// Find sqlite entry
			for (let i = 0; i <  files.length; i++) {
				let obj = files[i];
				
				// Find the sqlite entry
				if (obj.comment == sqliteLibrary + ' in Frameworks') {

					// Remove entry so we can re-place it later
					files.splice(i, 1);

					sqliteObj = obj;
					break;
				}
			}

			// Find our module library index
			for (let i = 0; i <  files.length; i++) {
				let obj = files[i];
				
				// Place sqlite entry above our module library entry
				if (obj.comment == `lib${exports.id}.a in Frameworks`) {
					files.splice(i, 0, sqliteObj);
					break;
				}
			}
						
			// Re-assign the re-arranged list of frameworks
			data.args[0].hash.project.objects.PBXFrameworksBuildPhase[PBXNativeTargetUUID].files = files;	
		}
	});
}
