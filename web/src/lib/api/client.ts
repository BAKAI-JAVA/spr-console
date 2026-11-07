export class ApiError extends Error {
	constructor(
		readonly status: number,
		message: string,
		/** The field errors of a refused change, e.g. "rules[0].conditions[1].value: …" */
		readonly errors: string[] = []
	) {
		super(message);
		this.name = 'ApiError';
	}
}

/** The fetch SvelteKit hands a load function: on the server it goes through hooks.server.ts. */
export type Fetch = typeof globalThis.fetch;

type Query = Record<string, string | number | boolean | null | undefined>;

type RequestOptions = {
	method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
	query?: Query;
	body?: unknown;
	fetch?: Fetch;
};

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
	const { method = 'GET', query, body, fetch: fetcher = globalThis.fetch } = options;

	let response: Response;
	try {
		// The API is on this app's own origin: this app's server passes /api/v1/admin on to it
		// (src/hooks.server.ts), with the author of the change.
		response = await fetcher(apiPath(path, query), {
			method,
			credentials: 'same-origin',
			headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
			body: body === undefined ? undefined : JSON.stringify(body)
		});
	} catch {
		throw new ApiError(0, 'Сервер недоступен. Запущен ли бэкенд стоп-факторов?');
	}

	const payload: unknown =
		response.status === 204 ? undefined : await response.json().catch(() => undefined);

	if (!response.ok) throw failureOf(response.status, payload);

	return payload as T;
}

/** The API's answer to a failed call: a ProblemDetail, read only as far as it has that shape. */
export function failureOf(status: number, payload: unknown): ApiError {
	const body = isRecord(payload) ? payload : {};
	const errors = Array.isArray(body.errors) ? body.errors.map(String) : [];
	const message =
		typeof body.detail === 'string'
			? body.detail
			: status === 502 || status === 504
				? 'Бэкенд стоп-факторов не отвечает'
				: 'Что-то пошло не так';

	return new ApiError(status, message, errors);
}

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function apiPath(path: string, query: Query = {}): string {
	const params = new URLSearchParams();
	for (const [key, value] of Object.entries(query)) {
		if (value !== undefined && value !== null && value !== '') params.set(key, String(value));
	}
	const search = params.toString();
	return `/api/v1/admin${path}${search ? `?${search}` : ''}`;
}

export const api = {
	get: <T>(path: string, query?: Query, fetcher?: Fetch) =>
		request<T>(path, { query, fetch: fetcher }),
	post: <T>(path: string, body?: unknown, query?: Query) =>
		request<T>(path, { method: 'POST', body, query }),
	put: <T>(path: string, body?: unknown, query?: Query) =>
		request<T>(path, { method: 'PUT', body, query }),
	patch: <T>(path: string, query?: Query) => request<T>(path, { method: 'PATCH', query }),
	delete: <T>(path: string, query?: Query) => request<T>(path, { method: 'DELETE', query })
};
