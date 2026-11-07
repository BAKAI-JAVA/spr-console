<script lang="ts">
	import { resolve } from '$app/paths';
	import { BRAND } from '#lib/brand.js';
	import { IconButton, ThemeToggle, Thumb } from '#lib/components/ui/index.js';
	import { useShell } from '#lib/state/shell.svelte.js';
	import UserMenu from './UserMenu.svelte';

	type Props = { user: string };

	let { user }: Props = $props();

	const shell = useShell();
</script>

<header class:mini={shell.collapsed}>
	<div class="menu-control">
		<IconButton
			icon={shell.menuOpen ? 'ri-close-line' : 'ri-menu-line'}
			label={shell.menuOpen ? 'Закрыть меню' : 'Открыть меню'}
			size="sm"
			placement="right"
			onclick={() => shell.setMenu(!shell.menuOpen)}
		/>
	</div>

	<div class="brand-column">
		<a class="brand" href={resolve('/(panel)/stop-factors')} aria-label="{BRAND.name}: консоль">
			<Thumb src={BRAND.favicon} text="СФ" size="sm" bare />

			<span class="title">
				<span class="wordmark" title={BRAND.name}>{BRAND.name}</span>
				<span class="divider" aria-hidden="true">/</span>
				<span class="product">Консоль</span>
			</span>
		</a>
	</div>

	<div class="fold-control">
		<IconButton
			icon={shell.collapsed ? 'ri-sidebar-unfold-line' : 'ri-sidebar-fold-line'}
			label={shell.collapsed ? 'Развернуть меню' : 'Свернуть меню'}
			size="sm"
			onclick={shell.toggle}
		/>
	</div>

	<div class="end">
		<ThemeToggle size="sm" variant="ghost" />
		<span class="rule" aria-hidden="true"></span>
		<UserMenu {user} />
	</div>
</header>

<style>
	header {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 10;
		display: flex;
		align-items: center;
		gap: var(--space-2);
		height: var(--header-height);
		padding-right: var(--page-gutter);
		border-bottom: 1px solid var(--color-border);
		background: var(--color-surface);
		overscroll-behavior: none;
	}

	.brand-column {
		display: flex;
		flex-shrink: 0;
		align-items: center;
		align-self: stretch;
		width: var(--sidebar-width);
		min-width: 0;
		overflow: hidden;
		border-right: 1px solid var(--color-border);
		background: var(--nav-surface);
	}

	.brand {
		display: flex;
		align-items: center;
		gap: 10px;
		min-width: 0;
		height: 100%;
		/* Centres the 34px logo in a folded, 56px sidebar, so it stays put. */
		padding: 0 var(--space-2) 0 11px;
		color: var(--color-text);
		text-decoration: none;
		white-space: nowrap;
	}

	.brand:focus-visible {
		outline: 2px solid var(--color-info);
		outline-offset: -4px;
	}

	/* The product's name, then, smaller and quieter, what this app is. */
	.title {
		display: flex;
		align-items: baseline;
		gap: 6px;
		min-width: 0;
		line-height: 1;
		transition: opacity var(--speed);
	}

	/* Tall enough for every letter's ascender and descender: the box clips
	   whatever it does not hold, so a long name can end in an ellipsis. */
	.wordmark {
		overflow: hidden;
		padding-block: 2px;
		line-height: 1.3;
		text-overflow: ellipsis;
		color: var(--color-text);
		font-size: var(--text-xl);
		font-weight: 500;
		letter-spacing: -0.02em;
	}

	.divider,
	.product {
		flex-shrink: 0;
		font-size: 10px;
		font-weight: 500;
		letter-spacing: 0.12em;
		text-transform: uppercase;
	}

	.divider {
		color: var(--color-text-disabled);
	}

	.product {
		color: var(--color-text-hint);
	}

	.end {
		display: flex;
		flex-shrink: 0;
		align-items: center;
		gap: 2px;
		margin-left: auto;
	}

	.rule {
		width: 1px;
		height: 20px;
		margin: 0 var(--space-2);
		background: var(--color-border);
	}

	@media (min-width: 55.0625rem) {
		.menu-control {
			display: none;
		}

		.mini .title {
			opacity: 0;
		}
	}

	@media (max-width: 55rem) {
		header {
			gap: var(--space-1);
			padding-left: var(--space-2);
		}

		.fold-control {
			display: none;
		}

		.brand-column {
			width: auto;
			flex-shrink: 1;
			border-right-color: transparent;
			background: none;
		}

		.brand {
			gap: var(--space-2);
			padding: 0 var(--space-1);
		}

		.rule {
			margin: 0 var(--space-1);
		}
	}

	/* A phone's header has no room for the second part. */
	@media (max-width: 40rem) {
		.divider,
		.product {
			display: none;
		}
	}
</style>
