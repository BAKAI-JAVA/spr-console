<script lang="ts">
	import type { StopFactor } from '#lib/api/index.js';
	import { Badge, CellText, DataTable, IconButton, type Column } from '#lib/components/ui/index.js';
	import { SEGMENTS, statusLabel } from '#lib/stop-factors/labels.js';
	import { rulesSummary } from '#lib/stop-factors/rules.js';
	import { countOf } from '#lib/utils/format.js';

	type Props = {
		stopFactors: StopFactor[];
		onOpen: (sf: StopFactor) => void;
		/** Shown while the order is edited: moves a row up (-1) or down (1). */
		onMove?: (index: number, delta: number) => void;
	};

	let { stopFactors, onOpen, onMove }: Props = $props();

	const columns = $derived<Column[]>([
		{ key: 'order', label: '№', min: '3.5rem' },
		{ key: 'name', label: 'Стоп-фактор', min: '18rem', grow: true },
		{ key: 'metric', label: 'Метрика', min: '14rem' },
		{ key: 'segment', label: 'Сегмент', min: '9rem' },
		{ key: 'rules', label: 'Правила', min: '7rem' },
		{ key: 'status', label: 'Статус', min: '8rem' },
		...(onMove ? [{ key: 'move', label: '', min: '5rem', align: 'end' as const }] : [])
	]);

	const rows = $derived(stopFactors.map((sf) => ({ id: String(sf.factorId), sf })));
</script>

<DataTable
	{columns}
	{rows}
	empty="Стоп-факторов по этим условиям нет."
	onOpen={onMove ? undefined : (row) => onOpen(row.sf)}
	label={(row) => `Открыть: ${row.sf.name}`}
>
	{#snippet row({ sf })}
		<td><span class="mono">{sf.orderNumber}</span></td>
		<td>
			<CellText
				title={sf.name}
				detail={sf.productIds.length
					? `#${sf.factorId} · ${countOf(sf.productIds.length, 'продукт', 'продукта', 'продуктов')}`
					: `#${sf.factorId}`}
			/>
		</td>
		<td><span class="muted">{sf.metricTitle}</span></td>
		<td>
			{#if sf.segment}<Badge>{SEGMENTS[sf.segment]}</Badge>{:else}<span class="muted">Все</span
				>{/if}
		</td>
		<td><span class="muted">{rulesSummary(sf)}</span></td>
		<td>
			<Badge tone={sf.active ? 'success' : 'neutral'}>{statusLabel(sf.active)}</Badge>
		</td>
		{#if onMove}
			{@const index = stopFactors.indexOf(sf)}
			<td class="end">
				<span class="move">
					<IconButton
						icon="ri-arrow-up-line"
						label="Выше"
						size="sm"
						disabled={index === 0}
						onclick={() => onMove(index, -1)}
					/>
					<IconButton
						icon="ri-arrow-down-line"
						label="Ниже"
						size="sm"
						disabled={index === stopFactors.length - 1}
						onclick={() => onMove(index, 1)}
					/>
				</span>
			</td>
		{/if}
	{/snippet}
</DataTable>

<style>
	.mono {
		font-family: var(--font-mono);
		font-size: var(--text-sm);
		color: var(--color-text-hint);
	}

	.muted {
		color: var(--color-text-hint);
		font-size: var(--text-sm);
	}

	.move {
		display: inline-flex;
		gap: 2px;
	}
</style>
