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

			// Wipe libsqlite3.dylib from the build files
			var PBXBuildFile = objects['PBXBuildFile'];
			for (var prop in PBXBuildFile) {
				if (PBXBuildFile[prop].fileRef_comment && PBXBuildFile[prop].fileRef_comment == sqliteLibrary) {
					delete PBXBuildFile[prop];
					delete PBXBuildFile[prop + '_comment'];
					break;
				}
			}

			// Wipe libsqlite3.dylib from build file references
			var PBXFileReference = objects['PBXFileReference'];
			for (var prop in PBXFileReference) {
				if (PBXFileReference[prop].name && PBXFileReference[prop].name == sqliteLibrary) {
					delete PBXFileReference[prop];
					delete PBXFileReference[prop + '_comment'];
					break;
				}
			}

			// Wipe libsqlite3.dylib from the frameworks
			var PBXGroup = objects['PBXGroup'];
			for (var prop in PBXGroup) {
				if (PBXGroup[prop].name && PBXGroup[prop].name == 'Frameworks') {
					var groupChildren = PBXGroup[prop].children;
					for (var x = 0; x < groupChildren.length; x++) {
						if (groupChildren[x].comment && groupChildren[x].comment == sqliteLibrary) {
							groupChildren.splice(x, 1);
							break;
						}
					}
					break;
				}
			}

			var PBXProject = objects['PBXProject'];
			var PBXFrameworksBuildPhase = objects['PBXFrameworksBuildPhase'];
			var PBXProjectUUID = PBXProject[hash.project.rootObject];

			// Get the targets by using the project UUID
			targets = PBXProjectUUID['targets'];

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
				if (obj.comment == sqliteLibrary + ' in Frameworks') {
					// Remove it
					files.splice(i, 1);
					break;
				}
			}

			// Re-assign the re-arranged list of frameworks
			data.args[0].hash.project.objects.PBXFrameworksBuildPhase[PBXNativeTargetUUID].files = files;
		}
	});
}
