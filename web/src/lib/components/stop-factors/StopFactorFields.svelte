<script lang="ts">
	import type { Product } from '#lib/api/index.js';
	import {
		Checkbox,
		FieldGrid,
		FormSection,
		Input,
		Note,
		Select,
		SwitchField,
		Textarea
	} from '#lib/components/ui/index.js';
	import type { Fields } from '#lib/stop-factors/fields.js';
	import { ALL, segmentOptions } from '#lib/stop-factors/options.js';

	type Props = {
		fields: Fields;
		products: Product[];
		/** What an empty name becomes. */
		namePlaceholder?: string;
	};

	let { fields = $bindable(), products, namePlaceholder }: Props = $props();

	const segments = segmentOptions();

	// A product of another segment is never checked by a stop factor of this one.
	const visibleProducts = $derived(
		fields.segment
			? products.filter((p) => !p.segmentType || p.segmentType === fields.segment)
			: products
	);

	function toggle(id: number, checked: boolean) {
		fields.productIds = checked
			? [...fields.productIds, id]
			: fields.productIds.filter((x) => x !== id);
	}
</script>

<FormSection title="Описание" description="Как стоп-фактор называется в консоли и в отчётах.">
	<Input
		label="Название"
		bind:value={fields.name}
		maxlength={255}
		placeholder={namePlaceholder}
		hint={namePlaceholder ? undefined : 'Пустое — по метрике и правилам.'}
	/>
	<Textarea label="Описание" bind:value={fields.description} rows={2} />
</FormSection>

<FormSection
	title="Применение"
	description="Какие заявки проверяет стоп-фактор: по сегменту продукта и по списку продуктов."
>
	<FieldGrid>
		<Select
			label="Сегмент"
			value={fields.segment || ALL}
			options={segments}
			onChange={(value) => (fields.segment = value === ALL ? '' : (value as Fields['segment']))}
			hint="Без сегмента — заявки всех сегментов."
		/>
		<Input
			label="Порядок"
			bind:value={fields.orderNumber}
			inputmode="numeric"
			placeholder="В конец списка"
			hint="Только для отображения, на решение не влияет."
		/>
	</FieldGrid>

	<div class="products" role="group" aria-label="Продукты">
		<span class="caption">Продукты</span>
		{#each visibleProducts as product (product.id)}
			<Checkbox
				label={product.name}
				checked={fields.productIds.includes(product.id)}
				onChange={(checked) => toggle(product.id, checked)}
			/>
		{:else}
			<Note>Нет продуктов этого сегмента.</Note>
		{/each}
	</div>
	<Note>Ни один продукт не отмечен — стоп-фактор проверяет все продукты сегмента.</Note>

	<SwitchField
		label="Активен"
		description="Неактивный стоп-фактор хранится, но не проверяет заявки."
		bind:checked={fields.active}
	/>
</FormSection>

<style>
	.products {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr));
		gap: var(--space-2) var(--space-4);
	}

	.caption {
		grid-column: 1 / -1;
		color: var(--color-text-hint);
		font-size: var(--text-sm);
		font-weight: 500;
	}
</style>
