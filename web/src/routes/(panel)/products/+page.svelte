<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { pageTitle } from '#lib/brand.js';
	import {
		Badge,
		CellText,
		DataTable,
		PageHeader,
		SearchInput,
		Toolbar,
		type Column
	} from '#lib/components/ui/index.js';
	import { SEGMENTS } from '#lib/stop-factors/labels.js';
	import { formatDate } from '#lib/utils/format.js';
	import { debounce, setSearchParams } from '#lib/utils/url.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	let search = $derived(data.search);
	const searchLater = debounce(() => setSearchParams({ search }));

	const columns: Column[] = [
		{ key: 'id', label: 'ID', min: '4rem' },
		{ key: 'name', label: 'Продукт', min: '20rem', grow: true },
		{ key: 'segment', label: 'Сегмент', min: '10rem' },
		{ key: 'updated', label: 'Обновлён', min: '8rem' }
	];

	const rows = $derived.by(() => {
		const words = data.search.trim().toLowerCase();
		return data.products
			.filter(
				(p) => !words || `${p.id} ${p.name} ${p.description ?? ''}`.toLowerCase().includes(words)
			)
			.map((product) => ({ id: String(product.id), product }));
	});
</script>

<svelte:head><title>{pageTitle('Продукты')}</title></svelte:head>

<PageHeader
	crumbs={['Справочники', 'Продукты']}
	count={data.products.length}
	description="Кредитные продукты СПР, только для чтения. Сегмент продукта решает, какие стоп-факторы проверяют его заявки."
/>

<Toolbar>
	<SearchInput
		label="Поиск продуктов"
		placeholder="Название, описание или ID…"
		bind:value={search}
		onsubmit={() => setSearchParams({ search })}
		oninput={searchLater}
	/>
</Toolbar>

<DataTable
	{columns}
	{rows}
	empty="Продуктов не найдено."
	onOpen={(row) => goto(resolve('/(panel)/products/[id]', { id: row.id }))}
	label={(row) => `Открыть: ${row.product.name}`}
>
	{#snippet row({ product })}
		<td><span class="mono">{product.id}</span></td>
		<td><CellText title={product.name} detail={product.description ?? undefined} /></td>
		<td>
			{#if product.segmentType}
				<Badge>{SEGMENTS[product.segmentType]}</Badge>
			{:else}
				<span class="muted">—</span>
			{/if}
		</td>
		<td><span class="muted">{formatDate(product.updatedDate ?? product.createdDate)}</span></td>
	{/snippet}
</DataTable>

<style>
	.mono {
		color: var(--color-text-hint);
		font-family: var(--font-mono);
		font-size: var(--text-sm);
	}

	.muted {
		color: var(--color-text-hint);
		font-size: var(--text-sm);
	}
</style>
