<script>
  import { createEventDispatcher } from 'svelte';
  const dispatch = createEventDispatcher();

  // Onboarding modes: 'otp' or 'file'
  let onboardingMode = 'otp';

  // State fields
  let phoneNumber = '';
  let otpCode = '';
  let otpSent = false;
  let username = '';
  let dailyLimit = 15;
  let useProxy = true;
  let proxyHost = '';
  let proxyPort = '';
  let proxyUser = '';
  let proxyPass = '';
  let proxyProtocol = 'SOCKS5';

  // Anti-fraud module settings
  let randomPauses = true;
  let typingSignal = true;

  // File Upload fields
  let uploadedFile = null;
  let fileError = '';
  let successMessage = '';
  let errorMessage = '';

  function handleSendOtp() {
    if (!phoneNumber) {
      errorMessage = 'Phone number is required for OTP verification.';
      return;
    }
    errorMessage = '';
    // Simulate API request to send OTP
    otpSent = true;
    successMessage = 'OTP code sent successfully to ' + phoneNumber;
    setTimeout(() => { successMessage = ''; }, 5000);
  }

  function handleOnboardOtp() {
    if (!phoneNumber || !otpCode) {
      errorMessage = 'Both Phone Number and OTP Code are required.';
      return;
    }
    errorMessage = '';

    // Create a mock onboarded account object
    const newAccount = {
      phone_number: phoneNumber,
      username: username || '@tg_user_' + Math.floor(Math.random() * 1000),
      status: 'ACTIVE',
      daily_limit: dailyLimit,
      proxy: useProxy ? {
        host: proxyHost || '127.0.0.1',
        port: parseInt(proxyPort) || 1080,
        protocol: proxyProtocol,
        username: proxyUser,
        password: proxyPass
      } : null,
      behavior_emulation: {
        random_pauses: randomPauses,
        typing_signal: typingSignal
      },
      onboarded_at: new Date().toISOString()
    };

    dispatch('onboard', newAccount);
    successMessage = 'Account ' + newAccount.username + ' onboarded successfully!';

    // Reset form
    phoneNumber = '';
    otpCode = '';
    otpSent = false;
    username = '';
    proxyHost = '';
    proxyPort = '';
    proxyUser = '';
    proxyPass = '';
  }

  function handleFileSelect(event) {
    const files = event.target.files;
    if (files && files.length > 0) {
      processSessionFile(files[0]);
    }
  }

  function handleFileDrop(event) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      processSessionFile(files[0]);
    }
  }

  function processSessionFile(file) {
    if (!file.name.endsWith('.session') && !file.name.endsWith('.json') && !file.name.includes('tdata')) {
      fileError = 'Invalid file format. Please upload .session, .json, or tdata folder/archive.';
      uploadedFile = null;
      return;
    }
    fileError = '';
    uploadedFile = file;
    errorMessage = '';
    successMessage = `File ${file.name} uploaded successfully. Preparing metadata extraction...`;
    setTimeout(() => { successMessage = ''; }, 5000);
  }

  function handleOnboardFile() {
    if (!uploadedFile) {
      errorMessage = 'Please select a session or tdata file to upload.';
      return;
    }
    errorMessage = '';

    const newAccount = {
      phone_number: '+1 (555) ' + Math.floor(100 + Math.random() * 900) + '-' + Math.floor(1000 + Math.random() * 9000),
      username: username || '@session_user_' + Math.floor(Math.random() * 1000),
      status: 'ACTIVE',
      daily_limit: dailyLimit,
      proxy: useProxy ? {
        host: proxyHost || '192.168.1.1',
        port: parseInt(proxyPort) || 1080,
        protocol: proxyProtocol,
        username: proxyUser,
        password: proxyPass
      } : null,
      behavior_emulation: {
        random_pauses: randomPauses,
        typing_signal: typingSignal
      },
      onboarded_at: new Date().toISOString()
    };

    dispatch('onboard', newAccount);
    successMessage = 'Account onboarded successfully from ' + uploadedFile.name;
    uploadedFile = null;
    username = '';
    proxyHost = '';
    proxyPort = '';
    proxyUser = '';
    proxyPass = '';
  }

  function cancel() {
    dispatch('cancel');
  }
</script>

<div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm max-w-2xl mx-auto animate-fadeIn" role="region" aria-labelledby="form-title">
  <div class="flex items-center justify-between mb-6">
    <div class="flex items-center gap-2">
      <span class="material-symbols-outlined text-primary text-2xl">person_add</span>
      <h2 id="form-title" class="font-headline-md text-headline-md text-primary">Onboard Telegram Session</h2>
    </div>
    <button on:click={cancel} class="text-on-surface-variant hover:text-primary transition-colors text-body-md font-body-md flex items-center gap-1 focus:outline-none focus:ring-2 focus:ring-primary rounded px-2 py-1">
      <span class="material-symbols-outlined text-sm">close</span> Cancel
    </button>
  </div>

  <!-- Tab Selection: OTP vs File Upload -->
  <div class="flex border-b border-outline-variant mb-6" role="tablist">
    <button
      role="tab"
      aria-selected={onboardingMode === 'otp'}
      class="flex-1 py-3 text-center font-semibold text-body-md border-b-2 transition-all focus:outline-none focus:text-primary
        {onboardingMode === 'otp' ? 'border-secondary text-secondary' : 'border-transparent text-on-surface-variant hover:text-primary'}"
      on:click={() => { onboardingMode = 'otp'; errorMessage = ''; successMessage = ''; }}
    >
      OTP Verification Code
    </button>
    <button
      role="tab"
      aria-selected={onboardingMode === 'file'}
      class="flex-1 py-3 text-center font-semibold text-body-md border-b-2 transition-all focus:outline-none focus:text-primary
        {onboardingMode === 'file' ? 'border-secondary text-secondary' : 'border-transparent text-on-surface-variant hover:text-primary'}"
      on:click={() => { onboardingMode = 'file'; errorMessage = ''; successMessage = ''; }}
    >
      Upload .session / tdata
    </button>
  </div>

  {#if errorMessage}
    <div class="p-3 mb-4 bg-error-container text-error rounded-lg text-xs flex items-center gap-2 animate-fadeIn" role="alert">
      <span class="material-symbols-outlined text-sm">error</span>
      {errorMessage}
    </div>
  {/if}

  {#if successMessage}
    <div class="p-3 mb-4 bg-[#e8f5e9] text-[#2e7d32] border border-[#a5d6a7] rounded-lg text-xs flex items-center gap-2 animate-fadeIn" role="status">
      <span class="material-symbols-outlined text-sm">check_circle</span>
      {successMessage}
    </div>
  {/if}

  <div class="space-y-6">
    <!-- MODE A: OTP VERIFICATION -->
    {#if onboardingMode === 'otp'}
      <div class="space-y-4 animate-fadeIn">
        <div class="flex flex-col gap-2">
          <label class="font-label-md text-label-md text-on-surface-variant" for="phone-input">Phone Number (International format)</label>
          <div class="flex gap-2">
            <input
              id="phone-input"
              bind:value={phoneNumber}
              type="tel"
              placeholder="+1234567890"
              class="flex-grow h-12 border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary outline-none bg-white font-body-md"
            />
            <button
              type="button"
              on:click={handleSendOtp}
              class="h-12 px-5 bg-secondary text-on-secondary rounded-lg font-semibold hover:opacity-90 active:scale-95 transition-all text-sm whitespace-nowrap"
            >
              Send OTP
            </button>
          </div>
        </div>

        {#if otpSent}
          <div class="flex flex-col gap-2 animate-fadeIn">
            <label class="font-label-md text-label-md text-on-surface-variant" for="otp-input">OTP Verification Code</label>
            <input
              id="otp-input"
              bind:value={otpCode}
              type="text"
              placeholder="Enter 5-digit code"
              maxlength="10"
              class="h-12 border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary outline-none bg-white font-mono text-center tracking-widest text-lg"
            />
          </div>
        {/if}
      </div>
    {/if}

    <!-- MODE B: FILE UPLOAD -->
    {#if onboardingMode === 'file'}
      <div class="space-y-4 animate-fadeIn">
        <div class="flex flex-col gap-2">
          <span class="font-label-md text-label-md text-on-surface-variant">Select session or tdata file</span>
          <div
            on:dragover|preventDefault
            on:drop={handleFileDrop}
            role="region"
            aria-label="Session File Upload Area"
            class="border-2 border-dashed border-outline-variant rounded-lg p-6 flex flex-col items-center justify-center bg-surface-container-low hover:bg-surface-container-low/80 hover:border-primary transition-all cursor-pointer relative"
          >
            <input
              type="file"
              accept=".session,.json,.zip"
              on:change={handleFileSelect}
              class="absolute inset-0 opacity-0 cursor-pointer"
              id="session-file-input"
            />
            <span class="material-symbols-outlined text-outline text-4xl mb-2">cloud_upload</span>
            <p class="font-body-lg text-body-lg text-on-surface-variant text-center">
              Drag & drop your <strong>.session</strong> or <strong>tdata archive</strong> here
            </p>
            <p class="text-xs text-outline mt-1 text-center">
              Supports Telethon/Pyrogram .session files or converted JSON credentials.
            </p>
          </div>
        </div>

        {#if fileError}
          <div class="p-3 bg-error-container text-error rounded-lg text-xs flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">error</span>
            {fileError}
          </div>
        {/if}

        {#if uploadedFile}
          <div class="p-3 bg-[#e8f5e9] text-[#2e7d32] border border-[#a5d6a7] rounded-lg text-xs flex items-center justify-between">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">check_circle</span>
              <span class="truncate">Selected: <strong>{uploadedFile.name}</strong></span>
            </div>
            <button on:click={() => { uploadedFile = null; fileError = ''; }} class="text-[#2e7d32] hover:underline focus:outline-none">Remove</button>
          </div>
        {/if}
      </div>
    {/if}

    <!-- COMMON PARAMETERS SECTION -->
    <div class="border-t border-outline-variant pt-6 space-y-4">
      <h3 class="text-body-lg font-bold text-primary">Session Configuration</h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex flex-col gap-2">
          <label class="font-label-md text-label-md text-on-surface-variant" for="username-input">Custom Username (Optional)</label>
          <input
            id="username-input"
            bind:value={username}
            type="text"
            placeholder="@example_user"
            class="h-11 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary outline-none bg-white"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label class="font-label-md text-label-md text-on-surface-variant" for="limit-input">Daily Messaging Rate Limit</label>
          <input
            id="limit-input"
            bind:value={dailyLimit}
            type="number"
            min="1"
            max="100"
            class="h-11 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary outline-none bg-white"
          />
          <span class="text-[11px] text-outline">Maximum cold contacts per 24h (rec. 15-20).</span>
        </div>
      </div>
    </div>

    <!-- PROXY BINDING SECTION -->
    <div class="border-t border-outline-variant pt-6 space-y-4">
      <div class="flex justify-between items-center">
        <span class="font-body-lg font-bold text-primary">Isolated Proxy Binding</span>
        <label class="flex items-center cursor-pointer gap-2">
          <input type="checkbox" bind:checked={useProxy} class="rounded border-outline-variant text-primary focus:ring-primary focus:ring-2" />
          <span class="text-sm font-semibold text-on-surface-variant">Enable assigned proxy</span>
        </label>
      </div>

      {#if useProxy}
        <div class="grid grid-cols-1 md:grid-cols-12 gap-3 animate-fadeIn">
          <div class="md:col-span-4 flex flex-col gap-2">
            <label class="font-label-sm text-label-sm text-on-surface-variant" for="protocol-select">Protocol</label>
            <div class="relative">
              <select
                id="protocol-select"
                bind:value={proxyProtocol}
                class="w-full h-11 appearance-none border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white pr-8 text-sm"
              >
                <option value="SOCKS5">SOCKS5</option>
                <option value="HTTP">HTTP</option>
              </select>
              <span class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant text-sm">expand_more</span>
            </div>
          </div>

          <div class="md:col-span-5 flex flex-col gap-2">
            <label class="font-label-sm text-label-sm text-on-surface-variant" for="host-input">Host / IP</label>
            <input
              id="host-input"
              bind:value={proxyHost}
              type="text"
              placeholder="12.34.56.78"
              class="h-11 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary outline-none bg-white text-sm"
            />
          </div>

          <div class="md:col-span-3 flex flex-col gap-2">
            <label class="font-label-sm text-label-sm text-on-surface-variant" for="port-input">Port</label>
            <input
              id="port-input"
              bind:value={proxyPort}
              type="text"
              placeholder="1080"
              class="h-11 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary outline-none bg-white text-sm"
            />
          </div>

          <div class="md:col-span-6 flex flex-col gap-2 mt-1">
            <label class="font-label-sm text-label-sm text-on-surface-variant" for="proxy-user-input">Proxy Username (Optional)</label>
            <input
              id="proxy-user-input"
              bind:value={proxyUser}
              type="text"
              placeholder="user123"
              class="h-11 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary outline-none bg-white text-sm"
            />
          </div>

          <div class="md:col-span-6 flex flex-col gap-2 mt-1">
            <label class="font-label-sm text-label-sm text-on-surface-variant" for="proxy-pass-input">Proxy Password (Optional)</label>
            <input
              id="proxy-pass-input"
              bind:value={proxyPass}
              type="password"
              placeholder="••••••••"
              class="h-11 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary outline-none bg-white text-sm"
            />
          </div>
        </div>
      {/if}
    </div>

    <!-- ANTI-FRAUD BEHAVIOR EMULATION -->
    <div class="border-t border-outline-variant pt-6 space-y-4">
      <span class="font-body-lg font-bold text-primary">Human Behavior Emulation</span>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Random Pauses -->
        <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
          <div class="flex flex-col gap-1">
            <span class="font-semibold text-body-md text-primary group-hover:text-secondary transition-colors">Randomized Pauses</span>
            <span class="text-xs text-on-surface-variant">Wait 120-300 sec between outbound tasks.</span>
          </div>
          <input
            type="checkbox"
            bind:checked={randomPauses}
            class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
          />
        </label>

        <!-- Typing Signal -->
        <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
          <div class="flex flex-col gap-1">
            <span class="font-semibold text-body-md text-primary group-hover:text-secondary transition-colors">"Typing..." Signal emulation</span>
            <span class="text-xs text-on-surface-variant">Send chat status signals before dispatch.</span>
          </div>
          <input
            type="checkbox"
            bind:checked={typingSignal}
            class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
          />
        </label>
      </div>
    </div>

    <!-- SUBMIT BUTTONS -->
    <div class="border-t border-outline-variant pt-6 flex gap-4">
      <button
        on:click={cancel}
        type="button"
        class="flex-1 h-12 bg-white border border-outline-variant rounded-lg text-on-surface-variant hover:bg-surface-container-low font-semibold transition-colors active:scale-95 flex items-center justify-center gap-1 focus:ring-2 focus:ring-primary focus:outline-none"
      >
        Cancel
      </button>

      <button
        on:click={onboardingMode === 'otp' ? handleOnboardOtp : handleOnboardFile}
        type="button"
        class="flex-[2] h-12 bg-primary text-on-primary rounded-lg font-bold shadow-md hover:opacity-95 transition-all flex items-center justify-center gap-2 active:scale-95 focus:ring-2 focus:ring-primary focus:outline-none"
      >
        <span class="material-symbols-outlined text-[18px]">verified_user</span>
        Verify & Complete Onboarding
      </button>
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
