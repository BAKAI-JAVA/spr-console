import { error } from '@sveltejs/kit';
import { productsApi, stopFactorsApi } from '#lib/api/index.js';
import { fromApi } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params, fetch }) => {
	if (!/^\d+$/.test(params.id)) error(404, 'Такого продукта нет');
	const id = Number(params.id);

	const [product, stopFactors] = await Promise.all([
		fromApi(productsApi.get(id, fetch)),
		fromApi(stopFactorsApi.list({ productId: id }, fetch))
	]);
	return { product, stopFactors };
};
