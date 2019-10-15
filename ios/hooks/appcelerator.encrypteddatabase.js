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
function init(logger, config, cli, _appc) {
	cli.on('build.ios.xcodeproject', {
		pre: function (data) {
			logger.info('Remove sqlite3.dylib...');

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
				// eslint-disable-next-line eqeqeq
				if (targets[i].comment == '"' + appName + '"') {
					PBXNativeTarget = objects['PBXNativeTarget'][targets[i].value];
					break;
				}
			}

			// Get the build phases related to this target
			buildPhases = PBXNativeTarget['buildPhases'];

			// Get the UUID of the target
			for (let i = 0; i < buildPhases.length; i++) {
				// eslint-disable-next-line eqeqeq
				if (buildPhases[i].comment == 'Frameworks') {
					PBXNativeTargetUUID = buildPhases[i].value;
					break;
				}
			}

			// Assign the target UUID to get the frameworks of the target
			files = PBXFrameworksBuildPhase[PBXNativeTargetUUID]['files'];

			// Find sqlite entry
			for (let i = 0; i <  files.length; i++) {
				let obj = files[i];

				// Find the sqlite entry
				// eslint-disable-next-line eqeqeq
				if (obj.comment == sqliteLibrary + ' in Frameworks') {

					// Remove entry, our module already contains sqlite3
					files.splice(i, 1);
					break;
				}
			}

			// Re-assign the re-arranged list of frameworks
			data.args[0].hash.project.objects.PBXFrameworksBuildPhase[PBXNativeTargetUUID].files = files;
		}
	});
}
