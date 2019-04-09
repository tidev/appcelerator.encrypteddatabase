/* global danger, fail, warn, message */

// requires
const junit = require('@seadub/danger-plugin-junit').default;
const dependencies = require('@seadub/danger-plugin-dependencies').default;

async function main() {
	// do a bunch of things in parallel
	// Specifically, anything that collects what labels to add or remove has to be done first before...
	await Promise.all([
		junit({ pathToReport: './TESTS-*.xml' }),
		dependencies({ type: 'npm' }),
	]);
}
main()
	.then(() => process.exit(0))
	.catch(err => {
		fail(err.toString());
		process.exit(1);
	});
