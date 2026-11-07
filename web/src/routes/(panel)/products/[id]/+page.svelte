<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import type { StopFactor } from '#lib/api/index.js';
	import { pageTitle } from '#lib/brand.js';
	import { Badge, FormSection, Note, PageHeader } from '#lib/components/ui/index.js';
	import StopFactorTable from '#lib/components/stop-factors/StopFactorTable.svelte';
	import { SEGMENTS } from '#lib/stop-factors/labels.js';
	import { formatDateTime } from '#lib/utils/format.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	const product = $derived(data.product);
	const active = $derived(data.stopFactors.filter((sf) => sf.active).length);

	function open(sf: StopFactor) {
		void goto(resolve('/(panel)/stop-factors/[factorId]', { factorId: String(sf.factorId) }));
	}
</script>

<svelte:head><title>{pageTitle(product.name, 'Продукты')}</title></svelte:head>

<div class="page">
	<PageHeader
		crumbs={[
			'Справочники',
			{ label: 'Продукты', href: resolve('/(panel)/products') },
			product.name
		]}
		description={product.description ?? undefined}
	>
		{#snippet meta()}
			<Badge>{product.segmentType ? SEGMENTS[product.segmentType] : 'Без сегмента'}</Badge>
			<Badge>ID {product.id}</Badge>
		{/snippet}
	</PageHeader>

	<FormSection
		title="Продукт"
		description="Справочник СПР: изменяется в СПР, не здесь."
		icon="ri-stack-line"
	>
		<dl>
			<dt>Сегмент</dt>
			<dd>{product.segmentType ? SEGMENTS[product.segmentType] : '—'}</dd>
			<dt>Создан</dt>
			<dd>{formatDateTime(product.createdDate)}</dd>
			<dt>Обновлён</dt>
			<dd>{formatDateTime(product.updatedDate)}</dd>
		</dl>
	</FormSection>

	<FormSection
		title="Стоп-факторы продукта"
		description="Проверяют заявки этого продукта: стоп-факторы его сегмента и без сегмента, без ограничения по продуктам или с этим продуктом в списке."
		icon="ri-shield-check-line"
	>
		{#snippet meta()}
			<Badge tone="success">Активных: {active}</Badge>
		{/snippet}
		{#if data.stopFactors.length}
			<StopFactorTable stopFactors={data.stopFactors} onOpen={open} />
		{:else}
			<Note>Ни один стоп-фактор не проверяет заявки этого продукта.</Note>
		{/if}
	</FormSection>
</div>

<style>
	.page {
		display: flex;
		flex-direction: column;
		gap: var(--space-4);
		padding-bottom: var(--space-6);
	}

	dl {
		display: grid;
		grid-template-columns: max-content 1fr;
		gap: var(--space-2) var(--space-5);
		margin: 0;
	}

	dt {
		color: var(--color-text-hint);
	}

	dd {
		margin: 0;
	}
</style>
