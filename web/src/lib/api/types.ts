// Types of the admin console API (core-console controllers, stopfactor console DTOs).

export type ProductSegment = 'RETAIL' | 'MSB';
export type StopFactorStatus = 'APPROVED' | 'REJECTED' | 'UNDERWRITER';
export type ConditionResult = Exclude<StopFactorStatus, 'APPROVED'>;
export type ValueType = 'STRING' | 'INTEGER' | 'DOUBLE' | 'ARRAY';
export type Operator = 'GT' | 'GTE' | 'LT' | 'LTE' | 'EQ' | 'RANGE' | 'BETWEEN' | 'IN';
export type ParamValue = string | number;

export interface Product {
	id: number;
	name: string;
	description: string | null;
	segmentType: ProductSegment | null;
	createdDate: string | null;
	updatedDate: string | null;
}

export interface OperatorOption {
	code: Operator;
	name: string;
}

export interface CalculatorParam {
	key: string;
	label: string;
	required: boolean;
	/** INTEGER or ENUM */
	type: string;
	min: number | null;
	allowedValues: string[] | null;
}

export interface Calculator {
	type: string;
	name: string;
	valueKind: 'NUMBER' | 'LIST';
	allowedConditionValueTypes: ValueType[];
	allowedOperators: OperatorOption[];
	params: CalculatorParam[];
}

export interface MetricParam {
	key: string;
	label: string;
	required: boolean;
}

export interface Metric {
	id: number | null;
	title: string;
	calculator: string;
	calculatorName?: string | null;
	nameTemplate: string;
	params: MetricParam[] | null;
	active: boolean | null;
	createdDate?: string | null;
	updatedDate?: string | null;
}

export interface Condition {
	id?: number | null;
	result: ConditionResult;
	valueType: ValueType;
	value: string;
	operator?: Operator | null;
	valueTo?: string | null;
}

export interface Rule {
	id?: number | null;
	name?: string | null;
	params: Record<string, ParamValue>;
	conditions: Condition[];
	active?: boolean | null;
}

export interface StopFactor {
	factorId: number;
	versionId: number;
	versionNumber: number;
	current: boolean;
	name: string;
	description: string | null;
	active: boolean;
	orderNumber: number;
	metricId: number;
	metricTitle: string;
	calculator: string;
	productIds: number[];
	segment: ProductSegment | null;
	rules: Rule[];
	validFrom: string;
	validTo: string | null;
	changeReason: string | null;
	createdBy: string | null;
	createdDate: string | null;
}

export interface StopFactorRequest {
	metricId: number;
	name?: string | null;
	description?: string | null;
	orderNumber?: number | null;
	active?: boolean | null;
	productIds?: number[];
	segment?: ProductSegment | null;
	rules: Rule[];
	changeReason?: string | null;
}

export interface StopFactorFilter {
	active?: boolean;
	metricId?: number;
	productId?: number;
	segment?: ProductSegment;
}
