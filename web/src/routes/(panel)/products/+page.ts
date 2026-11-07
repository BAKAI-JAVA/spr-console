import { productsApi } from '#lib/api/index.js';
import { fromApi } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch, url }) => ({
	products: await fromApi(productsApi.list(fetch)),
	search: url.searchParams.get('search') ?? ''
});
