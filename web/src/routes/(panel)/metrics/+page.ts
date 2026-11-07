import { metricsApi, stopFactorsApi } from '#lib/api/index.js';
import { fromApi } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
	const [metrics, calculators, stopFactors] = await Promise.all([
		fromApi(metricsApi.list(fetch)),
		fromApi(metricsApi.calculators(fetch)),
		fromApi(stopFactorsApi.list({}, fetch))
	]);
	return { metrics, calculators, stopFactors };
};
