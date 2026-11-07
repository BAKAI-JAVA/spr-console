// The console API (core-console controllers): one function per controller method.
import { api, type Fetch } from './client';
import type {
	Calculator,
	Condition,
	ConditionResult,
	Metric,
	Product,
	Rule,
	StopFactor,
	StopFactorFilter,
	StopFactorRequest
} from './types';

export const productsApi = {
	list: (fetch?: Fetch) => api.get<Product[]>('/products', undefined, fetch),
	get: (id: number, fetch?: Fetch) => api.get<Product>(`/products/${id}`, undefined, fetch)
};

export const metricsApi = {
	calculators: (fetch?: Fetch) => api.get<Calculator[]>('/metrics/calculators', undefined, fetch),
	list: (fetch?: Fetch) => api.get<Metric[]>('/metrics', undefined, fetch),
	get: (id: number, fetch?: Fetch) => api.get<Metric>(`/metrics/${id}`, undefined, fetch),
	create: (metric: Metric) => api.post<Metric>('/metrics', metric),
	update: (id: number, metric: Metric) => api.put<Metric>(`/metrics/${id}`, metric),
	delete: (id: number) => api.delete<void>(`/metrics/${id}`)
};

const factor = (factorId: number) => `/stop-factors/${factorId}`;
const rule = (factorId: number, ruleId: number) => `${factor(factorId)}/rules/${ruleId}`;

export const stopFactorsApi = {
	list: (filter: StopFactorFilter = {}, fetch?: Fetch) =>
		api.get<StopFactor[]>('/stop-factors', { ...filter }, fetch),
	get: (factorId: number, fetch?: Fetch) => api.get<StopFactor>(factor(factorId), undefined, fetch),
	versions: (factorId: number, fetch?: Fetch) =>
		api.get<StopFactor[]>(`${factor(factorId)}/versions`, undefined, fetch),
	create: (body: StopFactorRequest) => api.post<StopFactor>('/stop-factors', body),
	update: (factorId: number, body: StopFactorRequest) =>
		api.put<StopFactor>(factor(factorId), body),
	changeStatus: (factorId: number, active: boolean, changeReason: string) =>
		api.patch<StopFactor>(`${factor(factorId)}/status`, { active, changeReason }),
	reorder: (factorIds: number[]) => api.put<StopFactor[]>('/stop-factors/order', factorIds),
	delete: (factorId: number, changeReason: string) =>
		api.delete<void>(factor(factorId), { changeReason })
};

export const rulesApi = {
	add: (factorId: number, body: Rule, changeReason: string) =>
		api.post<StopFactor>(`${factor(factorId)}/rules`, body, { changeReason }),
	update: (factorId: number, ruleId: number, body: Rule, changeReason: string) =>
		api.put<StopFactor>(rule(factorId, ruleId), body, { changeReason }),
	delete: (factorId: number, ruleId: number, changeReason: string) =>
		api.delete<StopFactor>(rule(factorId, ruleId), { changeReason }),
	saveCondition: (factorId: number, ruleId: number, body: Condition, changeReason: string) =>
		api.put<StopFactor>(`${rule(factorId, ruleId)}/conditions`, body, { changeReason }),
	deleteCondition: (
		factorId: number,
		ruleId: number,
		result: ConditionResult,
		changeReason: string
	) => api.delete<StopFactor>(`${rule(factorId, ruleId)}/conditions/${result}`, { changeReason })
};
