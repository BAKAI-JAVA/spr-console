<script lang="ts">
	import { resolve } from '$app/paths';
	import type { Metric } from '#lib/api/index.js';
	import { pageTitle } from '#lib/brand.js';
	import {
		Badge,
		Button,
		CellText,
		DataTable,
		PageHeader,
		type Column
	} from '#lib/components/ui/index.js';
	import MetricDialog from '#lib/components/metrics/MetricDialog.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	const columns: Column[] = [
		{ key: 'title', label: 'Метрика', min: '18rem', grow: true },
		{ key: 'template', label: 'Шаблон названия', min: '18rem' },
		{ key: 'params', label: 'Параметры', min: '14rem' },
		{ key: 'usage', label: 'Стоп-факторы', min: '8rem', align: 'end' },
		{ key: 'status', label: 'Статус', min: '8rem' }
	];

	const calculatorNames = $derived(new Map(data.calculators.map((c) => [c.type, c.name])));
	const usage = $derived(
		data.stopFactors.reduce(
			(counts, sf) => counts.set(sf.metricId, (counts.get(sf.metricId) ?? 0) + 1),
			new Map<number, number>()
		)
	);
	const rows = $derived(data.metrics.map((metric) => ({ id: String(metric.id), metric })));

	let open = $state(false);
	let editing = $state<Metric | null>(null);

	function edit(metric: Metric | null) {
		editing = metric;
		open = true;
	}
</script>

<svelte:head><title>{pageTitle('Метрики')}</title></svelte:head>

<PageHeader
	crumbs={['Решения', 'Метрики']}
	count={data.metrics.length}
	description="Метрика — что измеряет стоп-фактор: калькулятор по данным КИБ или банка и параметры, которыми настраивается каждое правило."
>
	{#snippet actions()}
		<Button icon="ri-add-line" onclick={() => edit(null)}>Новая метрика</Button>
	{/snippet}
</PageHeader>

<DataTable
	{columns}
	{rows}
	empty="Метрик пока нет."
	onOpen={(row) => edit(row.metric)}
	label={(row) => `Изменить: ${row.metric.title}`}
>
	{#snippet row({ metric })}
		<td>
			<CellText
				title={metric.title}
				detail={calculatorNames.get(metric.calculator) ?? metric.calculator}
			/>
		</td>
		<td><code>{metric.nameTemplate}</code></td>
		<td>
			<span class="params">
				{#each metric.params ?? [] as param (param.key)}
					<Badge>{param.label}{param.required ? '' : ' (необяз.)'}</Badge>
				{:else}
					<span class="muted">—</span>
				{/each}
			</span>
		</td>
		<td class="end">
			{#if usage.get(metric.id!)}
				<a
					class="usage"
					href={resolve(`/(panel)/stop-factors?metricId=${metric.id}`)}
					onclick={(event) => event.stopPropagation()}
				>
					{usage.get(metric.id!)}
				</a>
			{:else}
				<span class="muted">0</span>
			{/if}
		</td>
		<td>
			<Badge tone={metric.active ? 'success' : 'neutral'}>
				{metric.active ? 'Активна' : 'Отключена'}
			</Badge>
		</td>
	{/snippet}
</DataTable>

<MetricDialog bind:open metric={editing} calculators={data.calculators} />

<style>
	code {
		color: var(--color-text-hint);
		font-family: var(--font-mono);
		font-size: var(--text-sm);
		overflow-wrap: anywhere;
	}

	.params {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-1);
	}

	.muted {
		color: var(--color-text-hint);
		font-size: var(--text-sm);
	}

	.usage {
		font-family: var(--font-mono);
		font-size: var(--text-sm);
	}
</style>
