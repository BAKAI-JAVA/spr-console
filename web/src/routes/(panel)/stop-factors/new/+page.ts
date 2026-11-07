import { metricsApi, productsApi } from '#lib/api/index.js';
import { fromApi } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch, url }) => {
	const [metrics, calculators, products] = await Promise.all([
		fromApi(metricsApi.list(fetch)),
		fromApi(metricsApi.calculators(fetch)),
		fromApi(productsApi.list(fetch))
	]);
	const active = metrics.filter((m) => m.active);
	const asked = url.searchParams.get('metricId') ?? '';

	return {
		metrics: active,
		calculators,
		products,
		metricId: active.some((m) => String(m.id) === asked) ? asked : ''
	};
};
