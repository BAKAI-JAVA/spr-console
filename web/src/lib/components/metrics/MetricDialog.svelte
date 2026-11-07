<script lang="ts">
	import { refreshAll } from '$app/navigation';
	import {
		messagesOf,
		metricsApi,
		type Calculator,
		type Metric,
		type MetricParam
	} from '#lib/api/index.js';
	import {
		Button,
		ConfirmDialog,
		DangerZone,
		FormSection,
		FullscreenDialog,
		IconButton,
		Input,
		Note,
		ProblemList,
		Select,
		Switch,
		SwitchField,
		notify
	} from '#lib/components/ui/index.js';

	type Props = {
		open: boolean;
		/** The metric being edited; none for a new one. */
		metric: Metric | null;
		calculators: Calculator[];
	};

	let { open = $bindable(false), metric, calculators }: Props = $props();

	type Form = {
		title: string;
		calculator: string;
		nameTemplate: string;
		active: boolean;
		params: MetricParam[];
	};

	let form = $state<Form>(fill(null));
	let saving = $state(false);
	let problems = $state<string[]>([]);
	let confirmingDelete = $state(false);
	let deleting = $state(false);

	// The sheet starts from the metric each time it opens.
	$effect.pre(() => {
		if (open) {
			form = fill(metric);
			problems = [];
		}
	});

	const calculator = $derived(calculators.find((c) => c.type === form.calculator));
	const calculatorOptions = $derived(
		calculators.map((c) => ({ value: c.type, label: c.name, description: c.type }))
	);
	const unused = $derived(
		(calculator?.params ?? []).filter((spec) => !form.params.some((p) => p.key === spec.key))
	);

	function fill(source: Metric | null): Form {
		return {
			title: source?.title ?? '',
			calculator: source?.calculator ?? '',
			nameTemplate: source?.nameTemplate ?? '',
			active: source?.active ?? true,
			params: (source?.params ?? []).map((p) => ({ ...p }))
		};
	}

	function chooseCalculator(type: string) {
		form.calculator = type;
		const chosen = calculators.find((c) => c.type === type);
		form.params = (chosen?.params ?? []).map(({ key, label, required }) => ({
			key,
			label,
			required
		}));
		if (!form.title && chosen) form.title = chosen.name;
		if (!form.nameTemplate && chosen) {
			form.nameTemplate = [chosen.name, ...chosen.params.map((p) => `{${p.key}}`)].join(' ');
		}
	}

	const requiredByCalculator = (key: string) =>
		calculator?.params.find((spec) => spec.key === key)?.required ?? false;

	function addParam(key: string) {
		const spec = calculator?.params.find((p) => p.key === key);
		if (spec) form.params.push({ key: spec.key, label: spec.label, required: spec.required });
	}

	async function save() {
		saving = true;
		problems = [];
		const body: Metric = {
			id: metric?.id ?? null,
			title: form.title.trim(),
			calculator: form.calculator,
			nameTemplate: form.nameTemplate.trim(),
			params: form.params.map((p) => ({ key: p.key, label: p.label.trim(), required: p.required })),
			active: form.active
		};
		try {
			if (metric?.id) await metricsApi.update(metric.id, body);
			else await metricsApi.create(body);
			await refreshAll();
			notify.success(metric ? 'Метрика сохранена' : 'Метрика создана', body.title);
			open = false;
		} catch (err) {
			problems = messagesOf(err);
		} finally {
			saving = false;
		}
	}

	async function remove() {
		if (!metric?.id) return;
		deleting = true;
		try {
			await metricsApi.delete(metric.id);
			await refreshAll();
			notify.success('Метрика удалена', metric.title);
			confirmingDelete = false;
			open = false;
		} catch (err) {
			confirmingDelete = false;
			notify.error(err, 'Не удалось удалить метрику');
		} finally {
			deleting = false;
		}
	}
</script>

<FullscreenDialog
	bind:open
	title={metric ? metric.title : 'Новая метрика'}
	meta={metric ? metric.calculator : undefined}
>
	<FormSection
		title="Метрика"
		description="Что считает калькулятор и как называются правила стоп-факторов этой метрики."
		icon="ri-function-line"
	>
		<Select
			label="Калькулятор"
			value={form.calculator}
			options={calculatorOptions}
			placeholder="Выберите калькулятор"
			onChange={chooseCalculator}
			readOnly={!!metric}
			hint={metric ? 'Калькулятор не меняется: для другого создайте новую метрику.' : undefined}
			required
		/>
		<Input label="Название" bind:value={form.title} maxlength={255} required />
		<Input
			label="Шаблон названия правила"
			bind:value={form.nameTemplate}
			required
			hint="{'{ключ}'} — значение параметра; [ … ] — часть, которая выводится, только если все её параметры заданы."
		/>
	</FormSection>

	{#if calculator}
		<FormSection
			title="Параметры правил"
			description="Что задаётся в каждом правиле стоп-фактора. Обязательные параметры калькулятора убрать нельзя."
			icon="ri-equalizer-line"
		>
			{#each form.params as param, index (param.key)}
				<div class="param">
					<code>{param.key}</code>
					<div class="label">
						<Input label="Подпись" bind:value={param.label} compact required />
					</div>
					<Switch
						label="Обязательный"
						bind:checked={param.required}
						disabled={requiredByCalculator(param.key)}
					/>
					<IconButton
						icon="ri-close-line"
						label="Убрать параметр"
						size="sm"
						disabled={requiredByCalculator(param.key)}
						onclick={() => form.params.splice(index, 1)}
					/>
				</div>
			{:else}
				<Note>У калькулятора нет параметров: правило одно и без настроек.</Note>
			{/each}

			{#if unused.length}
				<div class="add">
					{#each unused as spec (spec.key)}
						<Button
							variant="subtle"
							size="sm"
							icon="ri-add-line"
							onclick={() => addParam(spec.key)}
						>
							{spec.label}
						</Button>
					{/each}
				</div>
			{/if}
		</FormSection>
	{/if}

	<FormSection title="Статус" description="Для неактивной метрики нельзя создать стоп-фактор.">
		<SwitchField
			label="Активна"
			description="Стоп-факторы этой метрики можно создавать и включать."
			bind:checked={form.active}
		/>
	</FormSection>

	<ProblemList {problems} />

	{#if metric}
		<DangerZone
			title="Удалить метрику"
			description="Можно, только пока ни одна версия стоп-фактора на неё не ссылается; иначе отключите её."
			label="Удалить"
			onclick={() => (confirmingDelete = true)}
		/>
	{/if}

	{#snippet actions()}
		<Button variant="subtle" onclick={() => (open = false)} disabled={saving}>Отмена</Button>
		<Button onclick={save} loading={saving} disabled={saving || !form.calculator}>
			{saving ? 'Сохранение…' : metric ? 'Сохранить' : 'Создать метрику'}
		</Button>
	{/snippet}
</FullscreenDialog>

<ConfirmDialog
	bind:open={confirmingDelete}
	title="Удалить метрику «{metric?.title}»?"
	description="Это нельзя отменить."
	confirmLabel="Удалить"
	busy={deleting}
	onConfirm={remove}
/>

<style>
	.param {
		display: grid;
		grid-template-columns: 11rem minmax(0, 1fr) auto auto;
		align-items: center;
		gap: var(--space-3);
	}

	.param code {
		overflow: hidden;
		color: var(--color-text-hint);
		font-family: var(--font-mono);
		font-size: var(--text-sm);
		text-overflow: ellipsis;
	}

	.add {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-2);
	}

	@media (max-width: 40rem) {
		.param {
			grid-template-columns: minmax(0, 1fr) auto;
		}

		.param code,
		.label {
			grid-column: 1 / -1;
		}
	}
</style>
