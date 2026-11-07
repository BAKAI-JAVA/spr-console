import type { ProductSegment, StopFactor, StopFactorRequest } from '#lib/api/index.js';
import { toRequestRule } from './rules';

/** The fields of a stop factor besides its rules, as a form holds them. */
export type Fields = {
	name: string;
	description: string;
	segment: ProductSegment | '';
	productIds: number[];
	active: boolean;
	orderNumber: string;
};

export function emptyFields(): Fields {
	return { name: '', description: '', segment: '', productIds: [], active: true, orderNumber: '' };
}

export function fieldsOf(stopFactor: StopFactor): Fields {
	return {
		name: stopFactor.name,
		description: stopFactor.description ?? '',
		segment: stopFactor.segment ?? '',
		productIds: [...stopFactor.productIds],
		active: stopFactor.active,
		orderNumber: String(stopFactor.orderNumber)
	};
}

/** The fields as a request sends them: an empty one is left to the backend's default. */
export function requestFields(fields: Fields) {
	return {
		name: fields.name.trim() || null,
		description: fields.description.trim() || null,
		segment: fields.segment || null,
		productIds: [...fields.productIds].sort((a, b) => a - b),
		active: fields.active,
		orderNumber: /^\d+$/.test(fields.orderNumber.trim()) ? Number(fields.orderNumber) : null
	};
}

/** Whether a form differs from the stop factor it was filled from. */
export function fieldsChanged(fields: Fields, stopFactor: StopFactor): boolean {
	return (
		JSON.stringify(requestFields(fields)) !== JSON.stringify(requestFields(fieldsOf(stopFactor)))
	);
}

/**
 * The PUT that saves new fields and keeps the rules as they are. A name or order left empty keeps
 * the current one: the backend would render a name from the rules and move the stop factor last.
 */
export function updateRequest(
	stopFactor: StopFactor,
	fields: Fields,
	changeReason: string
): StopFactorRequest {
	const request = requestFields(fields);

	return {
		metricId: stopFactor.metricId,
		...request,
		name: request.name ?? stopFactor.name,
		orderNumber: request.orderNumber ?? stopFactor.orderNumber,
		rules: stopFactor.rules.map(toRequestRule),
		changeReason
	};
}
