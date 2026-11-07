import { error } from '@sveltejs/kit';
import { metricsApi, productsApi, stopFactorsApi } from '#lib/api/index.js';
import { fromApi } from '#lib/api/load.js';
import type { PageLoad } from './$types';

const TABS = new Set(['rules', 'settings', 'history']);

export const load: PageLoad = async ({ params, fetch, url }) => {
	if (!/^\d+$/.test(params.factorId)) error(404, 'Такого стоп-фактора нет');
	const factorId = Number(params.factorId);

	const [stopFactor, versions, calculators, products] = await Promise.all([
		fromApi(stopFactorsApi.get(factorId, fetch)),
		fromApi(stopFactorsApi.versions(factorId, fetch)),
		fromApi(metricsApi.calculators(fetch)),
		fromApi(productsApi.list(fetch))
	]);
	const metric = await fromApi(metricsApi.get(stopFactor.metricId, fetch));
	const tab = url.searchParams.get('tab') ?? '';

	return {
		stopFactor,
		versions,
		metric,
		calculator: calculators.find((c) => c.type === metric.calculator),
		products,
		tab: TABS.has(tab) ? tab : 'rules'
	};
};
