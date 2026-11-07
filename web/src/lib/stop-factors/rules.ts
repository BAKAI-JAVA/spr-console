import type { Metric, Rule, StopFactor } from '#lib/api/index.js';
import { countOf } from '#lib/utils/format.js';

/** "Период с (месяц): 1 · Период по (месяц): 3": a rule's params in its metric's order. */
export function paramsText(rule: Rule, metric: Metric | undefined): string {
	const labels = new Map((metric?.params ?? []).map((p) => [p.key, p.label]));
	const keys = metric?.params?.map((p) => p.key) ?? Object.keys(rule.params);

	const parts = keys
		.filter((key) => rule.params[key] !== undefined && rule.params[key] !== null)
		.map((key) => `${labels.get(key) ?? key}: ${rule.params[key]}`);

	return parts.join(' · ') || 'Без параметров';
}

/** A rule as a request sends it: without its id, which every version gives anew. */
export function toRequestRule({ name, params, conditions, active }: Rule): Rule {
	return { name, params, conditions, active };
}

export const isActiveRule = (rule: Rule): boolean => rule.active !== false;

/** "4 правила", or "3 из 4 правил" when some are turned off. */
export function rulesSummary(stopFactor: Pick<StopFactor, 'rules'>): string {
	const total = stopFactor.rules.length;
	const active = stopFactor.rules.filter(isActiveRule).length;
	const all = countOf(total, 'правило', 'правила', 'правил');

	return active === total ? all : `${active} из ${all}`;
}
