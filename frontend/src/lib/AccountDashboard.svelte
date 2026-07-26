<script>
  import { createEventDispatcher } from 'svelte';
  const dispatch = createEventDispatcher();

  // Accept accounts as prop so the parent state is synced
  export let accounts = [];

  // Local helper for rotating statuses for testing
  const STATUS_LIST = ['ACTIVE', 'TEMPORARY_SPAM_BLOCK', 'PERMANENT_BAN', 'RE_AUTHORIZATION_REQUIRED'];

  // Reactive calculations for dashboard stats
  $: total = accounts.length;
  $: activeCount = accounts.filter(a => a.status === 'ACTIVE').length;
  $: spamBlockCount = accounts.filter(a => a.status === 'TEMPORARY_SPAM_BLOCK').length;
  $: permanentBanCount = accounts.filter(a => a.status === 'PERMANENT_BAN').length;
  $: reAuthCount = accounts.filter(a => a.status === 'RE_AUTHORIZATION_REQUIRED').length;

  // Aggregate health rating
  $: healthScore = total > 0 ? Math.round((activeCount / total) * 100) : 100;

  // Computed angles for simulated pie chart slices
  $: activePercent = total > 0 ? (activeCount / total) * 100 : 100;
  $: riskPercent = total > 0 ? (spamBlockCount / total) * 100 : 0;
  $: criticalPercent = total > 0 ? (permanentBanCount / total) * 100 : 0;
  $: reAuthPercent = total > 0 ? (reAuthCount / total) * 100 : 0;

  function triggerOnboard() {
    dispatch('onboardRequest');
  }

  function rotateStatus(index) {
    const account = accounts[index];
    const currentIdx = STATUS_LIST.indexOf(account.status);
    const nextIdx = (currentIdx + 1) % STATUS_LIST.length;
    accounts[index].status = STATUS_LIST[nextIdx];
    accounts = [...accounts]; // Trigger reactivity
    dispatch('updateAccounts', accounts);
  }

  function deleteAccount(index) {
    accounts.splice(index, 1);
    accounts = [...accounts];
    dispatch('updateAccounts', accounts);
  }

  function getStatusLabel(status) {
    switch (status) {
      case 'ACTIVE': return 'Active';
      case 'TEMPORARY_SPAM_BLOCK': return 'Spam-Block';
      case 'PERMANENT_BAN': return 'Banned';
      case 'RE_AUTHORIZATION_REQUIRED': return 'Re-auth Required';
      default: return status;
    }
  }

  function getStatusClasses(status) {
    switch (status) {
      case 'ACTIVE':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-400 dark:border-emerald-800/50';
      case 'TEMPORARY_SPAM_BLOCK':
        return 'bg-amber-100 text-amber-800 border-amber-200 dark:bg-amber-900/30 dark:text-amber-400 dark:border-amber-800/50';
      case 'PERMANENT_BAN':
        return 'bg-rose-100 text-rose-800 border-rose-200 dark:bg-rose-900/30 dark:text-rose-400 dark:border-rose-800/50';
      case 'RE_AUTHORIZATION_REQUIRED':
        return 'bg-sky-100 text-sky-800 border-sky-200 dark:bg-sky-900/30 dark:text-sky-400 dark:border-sky-800/50';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  }
</script>

<div class="space-y-gutter animate-fadeIn" role="region" aria-labelledby="dashboard-heading">

  <!-- Header Section -->
  <div class="mb-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
    <div>
      <h2 id="dashboard-heading" class="text-display-lg-mobile md:text-display-lg font-display-lg text-on-surface">Account Management</h2>
      <p class="text-body-md font-body-md text-on-surface-variant">Real-time Telegram account pool vitals and session onboarding controls.</p>
    </div>
    <button
      on:click={triggerOnboard}
      class="bg-secondary text-on-secondary px-6 py-2.5 rounded-lg flex items-center justify-center gap-2 font-semibold shadow-sm hover:opacity-90 active:scale-95 transition-all focus:outline-none focus:ring-2 focus:ring-primary"
    >
      <span class="material-symbols-outlined" data-icon="add">add</span>
      <span>Onboard Session</span>
    </button>
  </div>

  <!-- Bento Grid Layout -->
  <div class="grid grid-cols-1 lg:grid-cols-12 gap-gutter">

    <!-- Health Chart Card -->
    <div class="lg:col-span-8 bg-surface-container-lowest border border-outline-variant rounded-xl shadow-sm p-lg flex flex-col justify-between min-h-[380px]">
      <div class="flex justify-between items-start mb-md">
        <div>
          <h3 class="text-headline-md font-headline-md text-primary">System Vitality</h3>
          <p class="text-label-sm font-label-sm text-outline">Real-time aggregate health score</p>
        </div>
      </div>

      <!-- Chart Area -->
      <div class="flex-1 flex flex-col md:flex-row items-center justify-center gap-lg">

        <!-- Pie Chart Simulation -->
        <div class="relative w-44 h-44 md:w-56 md:h-56 flex items-center justify-center">
          <svg class="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
            <circle cx="18" cy="18" fill="transparent" r="16" stroke="#e2e8f0" stroke-width="4"></circle>
            <!-- Render active segment -->
            <circle cx="18" cy="18" fill="transparent" r="16" stroke="#10b981" stroke-dasharray="{activePercent}, 100" stroke-width="4"></circle>
            <!-- Render risk segment -->
            <circle cx="18" cy="18" fill="transparent" r="16" stroke="#f59e0b" stroke-dasharray="{riskPercent}, 100" stroke-dashoffset="-{activePercent}" stroke-width="4"></circle>
            <!-- Render critical segment -->
            <circle cx="18" cy="18" fill="transparent" r="16" stroke="#ef4444" stroke-dasharray="{criticalPercent}, 100" stroke-dashoffset="-{activePercent + riskPercent}" stroke-width="4"></circle>
            <!-- Render reauth segment -->
            <circle cx="18" cy="18" fill="transparent" r="16" stroke="#0ea5e9" stroke-dasharray="{reAuthPercent}, 100" stroke-dashoffset="-{activePercent + riskPercent + criticalPercent}" stroke-width="4"></circle>
          </svg>
          <div class="absolute inset-0 flex flex-col items-center justify-center">
            <span class="text-display-lg font-display-lg text-primary">{healthScore}%</span>
            <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">HEALTH SCORE</span>
          </div>
        </div>

        <!-- Stats Legend -->
        <div class="grid grid-cols-2 gap-md w-full md:w-auto">
          <div class="p-4 bg-surface-container rounded-lg border border-outline-variant">
            <div class="flex items-center gap-2 mb-1">
              <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              <span class="text-label-sm font-label-sm text-on-surface-variant">Active</span>
            </div>
            <p class="text-headline-md font-headline-md text-primary">{activeCount}</p>
          </div>

          <div class="p-4 bg-surface-container rounded-lg border border-outline-variant">
            <div class="flex items-center gap-2 mb-1">
              <span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
              <span class="text-label-sm font-label-sm text-on-surface-variant">Spam-Block</span>
            </div>
            <p class="text-headline-md font-headline-md text-primary">{spamBlockCount}</p>
          </div>

          <div class="p-4 bg-surface-container rounded-lg border border-outline-variant">
            <div class="flex items-center gap-2 mb-1">
              <span class="w-2.5 h-2.5 rounded-full bg-rose-500"></span>
              <span class="text-label-sm font-label-sm text-on-surface-variant">Banned</span>
            </div>
            <p class="text-headline-md font-headline-md text-primary">{permanentBanCount}</p>
          </div>

          <div class="p-4 bg-surface-container rounded-lg border border-outline-variant">
            <div class="flex items-center gap-2 mb-1">
              <span class="w-2.5 h-2.5 rounded-full bg-sky-500"></span>
              <span class="text-label-sm font-label-sm text-on-surface-variant">Re-auth</span>
            </div>
            <p class="text-headline-md font-headline-md text-primary">{reAuthCount}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions Card -->
    <div class="lg:col-span-4 flex flex-col gap-gutter">
      <div class="bg-primary-container text-on-primary rounded-xl p-lg shadow-lg relative overflow-hidden h-full flex flex-col justify-between min-h-[300px]">
        <div class="relative z-10">
          <h3 class="text-headline-md font-headline-md mb-2 text-white">Management Actions</h3>
          <p class="text-on-primary-container text-body-md font-body-md">Streamlined operations for session pools.</p>
        </div>
        <div class="relative z-10 grid grid-cols-1 gap-3 mt-lg">
          <button on:click={triggerOnboard} class="flex items-center justify-between p-3 bg-surface-container-lowest/10 hover:bg-surface-container-lowest/20 rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-secondary text-left">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-white" data-icon="person_add">person_add</span>
              <span class="text-body-md font-body-md text-white">Onboard New Account</span>
            </div>
            <span class="material-symbols-outlined text-sm text-white" data-icon="chevron_right">chevron_right</span>
          </button>
          <button class="flex items-center justify-between p-3 bg-surface-container-lowest/10 hover:bg-surface-container-lowest/20 rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-secondary text-left">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-white" data-icon="vpn_key">vpn_key</span>
              <span class="text-body-md font-body-md text-white">Manage System Proxies</span>
            </div>
            <span class="material-symbols-outlined text-sm text-white" data-icon="chevron_right">chevron_right</span>
          </button>
          <button class="flex items-center justify-between p-3 bg-surface-container-lowest/10 hover:bg-surface-container-lowest/20 rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-secondary text-left">
            <div class="flex items-center gap-3">
              <span class="material-symbols-outlined text-white" data-icon="rotate_left">rotate_left</span>
              <span class="text-body-md font-body-md text-white">Rotate Stale Proxies</span>
            </div>
            <span class="material-symbols-outlined text-sm text-white" data-icon="chevron_right">chevron_right</span>
          </button>
        </div>
        <!-- Decorative Element -->
        <div class="absolute -right-12 -bottom-12 opacity-10">
          <span class="material-symbols-outlined !text-[160px] text-white" data-icon="bolt">bolt</span>
        </div>
      </div>
    </div>

    <!-- Active Accounts Pool Directory -->
    <div class="lg:col-span-12 bg-surface-container-lowest border border-outline-variant rounded-xl shadow-sm overflow-hidden">
      <div class="p-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-outline-variant">
        <div>
          <h3 class="text-headline-md font-headline-md text-primary">Active Accounts Pool ({total})</h3>
          <p class="text-label-sm font-label-sm text-outline">Manage active connections, change proxy endpoints, or rotate statuses for testing.</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-outline font-semibold uppercase">Quick Legend:</span>
          <span class="w-2 h-2 rounded-full bg-emerald-500"></span> <span class="text-xs text-on-surface-variant mr-2">Active</span>
          <span class="w-2 h-2 rounded-full bg-amber-500"></span> <span class="text-xs text-on-surface-variant mr-2">Spam-Block</span>
          <span class="w-2 h-2 rounded-full bg-rose-500"></span> <span class="text-xs text-on-surface-variant">Banned</span>
        </div>
      </div>

      {#if total === 0}
        <div class="p-12 text-center flex flex-col items-center justify-center">
          <span class="material-symbols-outlined text-outline text-6xl mb-4">no_accounts</span>
          <h4 class="text-body-lg font-bold text-primary mb-1">No Telegram accounts onboarded yet</h4>
          <p class="text-body-md text-on-surface-variant max-w-sm">Please click "Onboard Session" to add a new account using OTP or session files.</p>
        </div>
      {:else}
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse" aria-label="Telegram Accounts Table">
            <thead>
              <tr class="border-b border-outline-variant bg-surface-container-low text-xs font-bold uppercase text-on-surface-variant">
                <th class="p-4" scope="col">Telegram User</th>
                <th class="p-4" scope="col">Phone Number</th>
                <th class="p-4" scope="col">Assigned Proxy IP</th>
                <th class="p-4 text-center" scope="col">Daily Limit</th>
                <th class="p-4" scope="col">Health Status</th>
                <th class="p-4 text-right" scope="col">Interactive Controls</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/60">
              {#each accounts as account, idx}
                <tr class="hover:bg-surface-container-low/40 transition-colors">
                  <td class="p-4">
                    <div class="flex items-center gap-3">
                      <div class="w-9 h-9 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center font-bold text-sm">
                        {account.username.replace('@', '').slice(0, 2).toUpperCase()}
                      </div>
                      <div class="flex flex-col">
                        <span class="font-semibold text-primary">{account.username}</span>
                        <span class="text-[11px] text-outline">Onboarded {new Date(account.onboarded_at || Date.now()).toLocaleDateString()}</span>
                      </div>
                    </div>
                  </td>
                  <td class="p-4 font-mono text-sm text-primary">{account.phone_number}</td>
                  <td class="p-4">
                    {#if account.proxy}
                      <div class="flex flex-col">
                        <span class="font-semibold text-body-md text-primary">{account.proxy.host}:{account.proxy.port}</span>
                        <span class="text-xs text-outline">{account.proxy.protocol} • {account.proxy.username || 'No Auth'}</span>
                      </div>
                    {:else}
                      <span class="text-xs text-rose-500 flex items-center gap-1 font-semibold">
                        <span class="material-symbols-outlined text-sm">warning</span> No Proxy Assigned
                      </span>
                    {/if}
                  </td>
                  <td class="p-4 text-center font-semibold text-primary">{account.daily_limit} / 24h</td>
                  <td class="p-4">
                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border {getStatusClasses(account.status)}">
                      <span class="w-1.5 h-1.5 rounded-full
                        {account.status === 'ACTIVE' ? 'bg-emerald-500' : ''}
                        {account.status === 'TEMPORARY_SPAM_BLOCK' ? 'bg-amber-500' : ''}
                        {account.status === 'PERMANENT_BAN' ? 'bg-rose-500' : ''}
                        {account.status === 'RE_AUTHORIZATION_REQUIRED' ? 'bg-sky-500' : ''}
                      "></span>
                      {getStatusLabel(account.status)}
                    </span>
                  </td>
                  <td class="p-4 text-right">
                    <div class="flex items-center justify-end gap-2">
                      <button
                        on:click={() => rotateStatus(idx)}
                        class="px-3 py-1.5 bg-surface border border-outline-variant text-on-surface-variant hover:text-primary hover:border-primary rounded-lg text-xs font-semibold flex items-center gap-1 transition-all focus:outline-none focus:ring-2 focus:ring-primary"
                        title="Rotates the account health status to verify color badges"
                      >
                        <span class="material-symbols-outlined text-xs">sync</span> Check Health
                      </button>
                      <button
                        on:click={() => deleteAccount(idx)}
                        class="p-2 text-outline hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/20 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-rose-500"
                        title="Remove session"
                      >
                        <span class="material-symbols-outlined text-sm">delete</span>
                      </button>
                    </div>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    </div>

  </div>
</div>

<style>
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fadeIn {
    animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
</style>
