import { error } from '@sveltejs/kit';

import { ApiError } from './client';

/**
 * Awaits an API call made by a load, and turns its failure into the error page with the API's own
 * sentence.
 */
export async function fromApi<T>(call: Promise<T>): Promise<T> {
	try {
		return await call;
	} catch (err) {
		if (!(err instanceof ApiError)) throw err;

		if (err.status === 0) error(503, 'Бэкенд стоп-факторов недоступен.');
		error(err.status, err.message);
	}
}
