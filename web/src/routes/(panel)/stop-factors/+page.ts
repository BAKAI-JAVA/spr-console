import { metricsApi, productsApi, stopFactorsApi } from '#lib/api/index.js';
import { fromApi } from '#lib/api/load.js';
import { parseFilter } from '#lib/stop-factors/filter.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url, fetch }) => {
	const filter = parseFilter(url.searchParams);

	const [stopFactors, metrics, products] = await Promise.all([
		fromApi(stopFactorsApi.list(filter, fetch)),
		fromApi(metricsApi.list(fetch)),
		fromApi(productsApi.list(fetch))
	]);

	return { filter, search: url.searchParams.get('search') ?? '', stopFactors, metrics, products };
};
