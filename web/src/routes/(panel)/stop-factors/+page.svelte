<script lang="ts">
	import { goto, refreshAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { stopFactorsApi, type StopFactor } from '#lib/api/index.js';
	import { pageTitle } from '#lib/brand.js';
	import {
		Button,
		EmptyState,
		IconButton,
		LinkButton,
		PageHeader,
		SearchInput,
		Select,
		Toolbar,
		notify
	} from '#lib/components/ui/index.js';
	import StopFactorTable from '#lib/components/stop-factors/StopFactorTable.svelte';
	import { isFiltered, matchesSearch } from '#lib/stop-factors/filter.js';
	import { ALL, segmentOptions, unlessAll, withAll } from '#lib/stop-factors/options.js';
	import { debounce, setSearchParams } from '#lib/utils/url.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	let search = $derived(data.search);

	const filtered = $derived(isFiltered(data.filter) || data.search !== '');
	const shown = $derived(data.stopFactors.filter((sf) => matchesSearch(sf, data.search)));

	const statusOptions = withAll('Любой статус', [
		{ value: 'active', label: 'Активные' },
		{ value: 'inactive', label: 'Отключённые' }
	]);
	const segments = segmentOptions();
	const metricOptions = $derived(
		withAll(
			'Все метрики',
			data.metrics.map((m) => ({ value: String(m.id), label: m.title }))
		)
	);
	const productOptions = $derived(
		withAll(
			'Любой продукт',
			data.products.map((p) => ({ value: String(p.id), label: p.name }))
		)
	);

	function filterBy(key: string, value: string) {
		void setSearchParams({ [key]: unlessAll(value) });
	}

	const status = $derived(
		data.filter.active === undefined ? ALL : data.filter.active ? 'active' : 'inactive'
	);

	/** The ids in the order being edited; null while not reordering. */
	let order = $state<number[] | null>(null);
	let saving = $state(false);
	let refreshing = $state(false);

	const byId = $derived(new Map(data.stopFactors.map((sf) => [sf.factorId, sf])));
	const ordered = $derived(
		order ? order.map((id) => byId.get(id)).filter((sf): sf is StopFactor => !!sf) : shown
	);

	const searchLater = debounce(() => setSearchParams({ search }));

	function move(index: number, delta: number) {
		if (!order) return;
		const target = index + delta;
		if (target < 0 || target >= order.length) return;
		[order[index], order[target]] = [order[target], order[index]];
	}

	async function saveOrder() {
		if (!order) return;
		saving = true;
		try {
			await stopFactorsApi.reorder(order);
			order = null;
			await refreshAll();
			notify.success('Порядок сохранён');
		} catch (err) {
			notify.error(err, 'Не удалось сохранить порядок');
		} finally {
			saving = false;
		}
	}

	async function refresh() {
		refreshing = true;
		try {
			await refreshAll();
		} finally {
			refreshing = false;
		}
	}

	function open(sf: StopFactor) {
		void goto(resolve('/(panel)/stop-factors/[factorId]', { factorId: String(sf.factorId) }));
	}
</script>

<svelte:head><title>{pageTitle()}</title></svelte:head>

<PageHeader
	crumbs={['Решения', 'Стоп-факторы']}
	count={data.stopFactors.length}
	description="Проверки кредитной заявки по данным КИБ и банка. Стоп-фактор считает метрику по каждому своему правилу и решает: отказ, андеррайтер или одобрение."
>
	{#snippet secondary()}
		<IconButton
			icon="ri-refresh-line"
			label="Обновить"
			onclick={refresh}
			loading={refreshing}
			disabled={refreshing}
		/>
	{/snippet}

	{#snippet actions()}
		{#if order}
			<Button variant="subtle" onclick={() => (order = null)} disabled={saving}>Отмена</Button>
			<Button onclick={saveOrder} loading={saving} disabled={saving} icon="ri-check-line">
				Сохранить порядок
			</Button>
		{:else}
			<Button
				variant="subtle"
				icon="ri-arrow-up-down-line"
				disabled={filtered || data.stopFactors.length < 2}
				title={filtered ? 'Сбросьте фильтры: порядок задаётся для всех стоп-факторов' : undefined}
				onclick={() => (order = data.stopFactors.map((sf) => sf.factorId))}
			>
				Порядок
			</Button>
			<LinkButton href={resolve('/(panel)/stop-factors/new')} icon="ri-add-line">
				Новый стоп-фактор
			</LinkButton>
		{/if}
	{/snippet}
</PageHeader>

{#if !order}
	<Toolbar>
		<SearchInput
			label="Поиск стоп-факторов"
			placeholder="Название, метрика или №…"
			bind:value={search}
			onsubmit={() => setSearchParams({ search })}
			oninput={searchLater}
		/>

		{#snippet end()}
			<div class="filters">
				<Select
					label="Статус"
					compact
					value={status}
					options={statusOptions}
					onChange={(value) => filterBy('status', value)}
				/>
				<Select
					label="Сегмент"
					compact
					value={data.filter.segment ?? ALL}
					options={segments}
					onChange={(value) => filterBy('segment', value)}
				/>
				<Select
					label="Метрика"
					compact
					value={data.filter.metricId ? String(data.filter.metricId) : ALL}
					options={metricOptions}
					onChange={(value) => filterBy('metricId', value)}
				/>
				<Select
					label="Проверяет продукт"
					compact
					value={data.filter.productId ? String(data.filter.productId) : ALL}
					options={productOptions}
					onChange={(value) => filterBy('productId', value)}
				/>
			</div>
		{/snippet}
	</Toolbar>
{/if}

{#if data.stopFactors.length === 0 && !filtered}
	<EmptyState icon="ri-shield-check-line" title="Стоп-факторов пока нет" size="lg">
		Создайте первый: выберите метрику и задайте правила с порогами отказа и андеррайтера.
		{#snippet actions()}
			<LinkButton href={resolve('/(panel)/stop-factors/new')} icon="ri-add-line">
				Новый стоп-фактор
			</LinkButton>
		{/snippet}
	</EmptyState>
{:else}
	<StopFactorTable stopFactors={ordered} onOpen={open} onMove={order ? move : undefined} />
{/if}

<style>
	.filters {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-2);
	}

	.filters :global(> *) {
		min-width: 11rem;
	}
</style>
