<script>
  import AccountDashboard from './lib/AccountDashboard.svelte';
  import AccountOnboardForm from './lib/AccountOnboardForm.svelte';

  // Application routing / view state
  // 'dashboard': Account Management Dashboard (Default)
  // 'campaigns': Existing Campaign Wizard
  // 'onboard': Account Onboarding Form
  let currentView = 'dashboard';

  // Global Svelte State for Telegram Accounts pool
  let accounts = [
    {
      phone_number: '+44 7911 123456',
      username: '@uk_crypto_agent',
      status: 'ACTIVE',
      daily_limit: 15,
      proxy: {
        host: '185.220.101.5',
        port: 1080,
        protocol: 'SOCKS5',
        username: 'socks_user',
        password: ''
      },
      behavior_emulation: { random_pauses: true, typing_signal: true },
      onboarded_at: '2026-07-20T10:30:00Z'
    },
    {
      phone_number: '+1 202 555 0143',
      username: '@us_growth_bot',
      status: 'TEMPORARY_SPAM_BLOCK',
      daily_limit: 20,
      proxy: {
        host: '45.89.22.180',
        port: 8080,
        protocol: 'HTTP',
        username: '',
        password: ''
      },
      behavior_emulation: { random_pauses: true, typing_signal: false },
      onboarded_at: '2026-07-22T14:15:00Z'
    },
    {
      phone_number: '+49 172 9876543',
      username: '@de_alpha_lead',
      status: 'RE_AUTHORIZATION_REQUIRED',
      daily_limit: 15,
      proxy: {
        host: '92.118.13.44',
        port: 1080,
        protocol: 'SOCKS5',
        username: 'admin',
        password: 'password'
      },
      behavior_emulation: { random_pauses: false, typing_signal: true },
      onboarded_at: '2026-07-24T09:00:00Z'
    }
  ];

  function handleOnboarded(event) {
    const newAcc = event.detail;
    accounts = [...accounts, newAcc];
    currentView = 'dashboard';
  }

  function handleUpdateAccounts(event) {
    accounts = event.detail;
  }

  // --- Campaign Setup State Variables ---
  let currentCampaignStep = 1; // 1: Basics, 2: Audience, 3: Content

  // Step 1: Basics fields
  let campaignName = "Crypto Dev Outreach Q3";
  let objective = "conversion";
  let deliveryChannels = {
    email: true,
    sms: false,
    push: true
  };
  let startDate = "2024-10-12";
  let endDate = "2024-10-26";
  let budget = 500;

  // Step 2: Audience / Lead Ingestion fields
  let targetLists = [
    { id: 1, name: "Solidity Developers" },
    { id: 2, name: "Telegram Group Inbounds" }
  ];
  let selectedTargetListId = 1;
  let newTargetListName = "";
  let showCreateTargetList = false;
  let uploadedFile = null;
  let fileError = "";
  let importSuccessMessage = "";
  let parsedLeads = []; // Array of leads parsed/imported manually or via CSV

  // Manual Lead fields
  let manualLeadUsername = "";
  let manualLeadPhone = "";
  let manualLeadFirstName = "";
  let manualLeadLastName = "";
  let manualLeadMetadata = "";

  // Step 3: Content fields
  let spintaxTemplate = "{Hi|Hello|Hey} {there|friend|}, check this out!";
  let useLlmPersonalization = false;
  let campaignStatus = "DRAFT"; // DRAFT, ACTIVE, PAUSED, COMPLETED

  // Spintax balancing check
  $: spintaxValid = checkSpintaxBalance(spintaxTemplate);
  $: spintaxError = spintaxValid ? "" : "Warning: Unbalanced brackets in Spintax template.";

  function checkSpintaxBalance(text) {
    let openCount = 0;
    for (let char of text) {
      if (char === '{') openCount++;
      if (char === '}') {
        openCount--;
        if (openCount < 0) return false;
      }
    }
    return openCount === 0;
  }

  // Insert spintax variable helper
  function insertVariable(variable) {
    spintaxTemplate += ` {${variable}}`;
  }

  // Navigation handlers
  function handleNext() {
    if (currentCampaignStep < 3) {
      currentCampaignStep += 1;
    } else {
      handleSaveCampaign();
    }
  }

  // Go to step directly
  function goToStep(step) {
    currentCampaignStep = step;
  }

  function handleBack() {
    if (currentCampaignStep > 1) {
      currentCampaignStep -= 1;
    }
  }

  // Lead List Ingestion actions
  function handleCsvDrop(event) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      processCsvFile(files[0]);
    }
  }

  function handleCsvSelect(event) {
    const files = event.target.files;
    if (files && files.length > 0) {
      processCsvFile(files[0]);
    }
  }

  function processCsvFile(file) {
    if (!file.name.endsWith('.csv') && !file.name.endsWith('.txt')) {
      fileError = "Invalid file type. Please upload a .csv or .txt file.";
      uploadedFile = null;
      return;
    }
    fileError = "";
    uploadedFile = file;

    // Simulate reading CSV rows
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target.result;
      const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
      let leadsCount = 0;
      parsedLeads = [];

      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(',').map(c => c.trim().replace(/^["']|["']$/g, ''));
        if (cols[0]) {
          parsedLeads.push({
            username: cols[0],
            phoneNumber: cols[1] || "",
            firstName: cols[2] || "",
            lastName: cols[3] || "",
            metadata: cols[4] || ""
          });
          leadsCount++;
        }
      }
      importSuccessMessage = `Successfully imported ${leadsCount} leads from ${file.name}.`;
    };
    reader.readAsText(file);
  }

  function handleAddManualLead() {
    if (!manualLeadUsername) {
      fileError = "Lead Username is required.";
      return;
    }
    fileError = "";
    parsedLeads = [
      ...parsedLeads,
      {
        username: manualLeadUsername,
        phoneNumber: manualLeadPhone,
        firstName: manualLeadFirstName,
        lastName: manualLeadLastName,
        metadata: manualLeadMetadata
      }
    ];
    manualLeadUsername = "";
    manualLeadPhone = "";
    manualLeadFirstName = "";
    manualLeadLastName = "";
    manualLeadMetadata = "";
    importSuccessMessage = "Lead added successfully to current batch.";
  }

  function handleCreateTargetList() {
    if (!newTargetListName) return;
    const newId = targetLists.length + 1;
    targetLists = [...targetLists, { id: newId, name: newTargetListName }];
    selectedTargetListId = newId;
    newTargetListName = "";
    showCreateTargetList = false;
  }

  // Create or Update Campaign
  let isSaving = false;
  let saveSuccess = false;

  async function handleSaveCampaign() {
    isSaving = true;
    saveSuccess = false;

    const payload = {
      name: campaignName,
      spintaxTemplate,
      useLlmPersonalization,
      status: campaignStatus,
      targetListId: selectedTargetListId
    };

    console.log("Saving campaign configurations:", payload);

    // Simulate API request to /api/campaigns
    setTimeout(() => {
      isSaving = false;
      saveSuccess = true;
    }, 1200);
  }


  // STATE FOR ACCOUNT MANAGEMENT DASHBOARD (Tab: "accounts")
  let accounts = [
    { id: 1, phoneNumber: "+12025550143", username: "stellar_bot", status: "ACTIVE", proxy: "SOCKS5://185.230.124.52:1080", dailyLimit: 15, isWarmedUp: true, currentTrustScore: 9.2, company: "Stellar Dynamics", joined: "Oct 2023" },
    { id: 2, phoneNumber: "+13125550198", username: "omnicorp_bot", status: "TEMPORARY_SPAM_BLOCK", proxy: "HTTP://192.168.12.100:8080", dailyLimit: 12, isWarmedUp: true, currentTrustScore: 4.8, company: "OmniCorp Systems", joined: "Jan 2024" },
    { id: 3, phoneNumber: "+14155550112", username: "nebula_bot", status: "RE_AUTHORIZATION_REQUIRED", proxy: "None", dailyLimit: 0, isWarmedUp: false, currentTrustScore: 0.0, company: "Nebula Labs", joined: "Onboarding" },
    { id: 4, phoneNumber: "+16175550175", username: "biopharma_bot", status: "ACTIVE", proxy: "SOCKS5://45.138.22.105:1080", dailyLimit: 20, isWarmedUp: true, currentTrustScore: 8.9, company: "BioPharma Ltd", joined: "May 2022" },
    { id: 5, phoneNumber: "+17185550121", username: "vertex_bot", status: "PERMANENT_BAN", proxy: "SOCKS5://93.115.26.11:1080", dailyLimit: 0, isWarmedUp: false, currentTrustScore: 1.5, company: "Vertex Finance", joined: "Sept 2023" }
  ];

  let searchQuery = "";
  let selectedFilter = "all"; // "all", "ACTIVE", "TEMPORARY_SPAM_BLOCK", "RE_AUTHORIZATION_REQUIRED", "PERMANENT_BAN"

  $: filteredAccounts = accounts.filter(acc => {
    // Filter by status
    if (selectedFilter !== "all" && acc.status !== selectedFilter) return false;
    // Filter by search query
    if (searchQuery.trim() !== "") {
      const query = searchQuery.toLowerCase();
      return (
        acc.username?.toLowerCase().includes(query) ||
        acc.phoneNumber?.toLowerCase().includes(query) ||
        acc.company?.toLowerCase().includes(query)
      );
    }
    return true;
  });

  // Onboarding Modal and Forms
  let showOnboardModal = false;
  let onboardMethod = "otp"; // "otp" or "file"

  // Form Fields
  let onboardPhone = "";
  let onboardUsername = "";
  let otpSent = false;
  let onboardOtp = "";
  let otpSending = false;
  let otpVerifying = false;
  let fileUploading = false;
  let uploadSuccessMessage = "";
  let onboardError = "";
  let onboardSuccess = false;

  // Proxy Settings
  let useProxy = true;
  let proxyProtocol = "SOCKS5";
  let proxyHost = "";
  let proxyPort = "";
  let proxyUser = "";
  let proxyPass = "";

  // Session upload file state
  let sessionFiles = [];
  let sessionFileError = "";

  function handleSendOtp() {
    if (!onboardPhone) {
      onboardError = "Phone number is required for OTP onboarding.";
      return;
    }
    onboardError = "";
    otpSending = true;

    // Simulate requesting OTP from Telegram API
    setTimeout(() => {
      otpSending = false;
      otpSent = true;
    }, 1000);
  }

  function handleVerifyOtp() {
    if (!onboardOtp) {
      onboardError = "Please enter the verification code.";
      return;
    }
    onboardError = "";
    otpVerifying = true;

    // Simulate OTP verification and session storage
    setTimeout(() => {
      otpVerifying = false;

      let proxyString = "None";
      if (useProxy && proxyHost && proxyPort) {
        proxyString = `${proxyProtocol}://${proxyHost}:${proxyPort}`;
      }

      // Add account to state
      const newAcc = {
        id: accounts.length + 1,
        phoneNumber: onboardPhone,
        username: onboardUsername || "new_tg_account",
        status: "ACTIVE",
        proxy: proxyString,
        dailyLimit: 15,
        isWarmedUp: false,
        currentTrustScore: 5.0,
        company: "Personal Onboarded",
        joined: "Just Now"
      };

      accounts = [newAcc, ...accounts];
      onboardSuccess = true;

      // Close modal after success
      setTimeout(() => {
        closeOnboardModal();
      }, 1500);
    }, 1200);
  }

  function handleSessionDrop(event) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      processSessionFiles(files);
    }
  }

  function handleSessionSelect(event) {
    const files = event.target.files;
    if (files && files.length > 0) {
      processSessionFiles(files);
    }
  }

  function processSessionFiles(files) {
    const validFiles = Array.from(files).filter(f => f.name.endsWith('.session') || f.name.includes('tdata'));
    if (validFiles.length === 0) {
      sessionFileError = "Invalid file type. Please upload .session or tdata files.";
      return;
    }
    sessionFileError = "";
    fileUploading = true;

    setTimeout(() => {
      fileUploading = false;
      sessionFiles = [...sessionFiles, ...validFiles];
      uploadSuccessMessage = `Successfully parsed ${validFiles.length} session file(s).`;

      // Automatically add them as active accounts
      validFiles.forEach((file, index) => {
        let proxyString = "None";
        if (useProxy && proxyHost && proxyPort) {
          proxyString = `${proxyProtocol}://${proxyHost}:${proxyPort}`;
        }

        const cleanName = file.name.replace('.session', '');
        const mockPhone = `+1555${Math.floor(100000 + Math.random() * 900000)}`;

        accounts = [{
          id: accounts.length + 1,
          phoneNumber: mockPhone,
          username: cleanName,
          status: "ACTIVE",
          proxy: proxyString,
          dailyLimit: 15,
          isWarmedUp: true,
          currentTrustScore: 7.2,
          company: "Imported Session",
          joined: "Just Now"
        }, ...accounts];
      });

      onboardSuccess = true;
      setTimeout(() => {
        closeOnboardModal();
      }, 1500);
    }, 1500);
  }

  function openOnboardModal() {
    showOnboardModal = true;
    onboardPhone = "";
    onboardUsername = "";
    onboardOtp = "";
    otpSent = false;
    onboardSuccess = false;
    onboardError = "";
    sessionFiles = [];
    sessionFileError = "";
    uploadSuccessMessage = "";
    proxyHost = "185.112.44.12";
    proxyPort = "1080";
    proxyUser = "";
    proxyPass = "";
  }

  function closeOnboardModal() {
    showOnboardModal = false;
  }
</script>

<div class="min-h-screen flex flex-col md:flex-row bg-[#f8f9ff] text-[#0b1c30] font-body-md text-body-md">

  <!-- Top App Bar Header -->
  <header class="fixed top-0 left-0 w-full bg-white z-40 flex justify-between items-center h-16 px-6 border-b border-outline-variant shadow-sm transition-all duration-200">
    <div class="flex items-center gap-4">
      <span class="material-symbols-outlined text-primary cursor-pointer active:scale-95 transition-transform" data-icon="menu">menu</span>
      <h1 class="text-headline-md font-headline-md text-primary font-semibold">LeadGen Bot Admin</h1>
    </div>
    <div class="flex items-center gap-4">
      <div class="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container font-bold shadow-sm">
        AD
      </div>
    </div>
  </header>

  <!-- Sidebar Navigation (Desktop Only) -->
  <aside class="hidden md:flex fixed inset-y-0 left-0 z-30 flex-col h-full w-64 bg-white border-r border-outline-variant shadow-sm pt-20">
    <div class="p-4 border-b border-outline-variant">
      <h2 class="text-body-lg font-bold text-primary">Operations</h2>
      <p class="text-label-sm font-label-sm text-on-surface-variant">LeadGen Control Hub</p>
    </div>
    <nav class="flex-1 py-4 overflow-y-auto space-y-1 px-2" aria-label="Sidebar Navigation">
      <button
        on:click={() => currentView = 'dashboard'}
        class="w-full text-left rounded-lg px-4 py-2.5 flex items-center gap-3 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary
          {currentView === 'dashboard' || currentView === 'onboard' ? 'bg-secondary-container text-on-secondary-container font-semibold shadow-sm' : 'text-on-surface-variant hover:bg-surface-container-low'}"
      >
        <span class="material-symbols-outlined" data-icon="analytics">analytics</span>
        <span class="text-body-md">Account Pool</span>
      </button>

      <button
        on:click={() => currentView = 'campaigns'}
        class="w-full text-left rounded-lg px-4 py-2.5 flex items-center gap-3 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary
          {currentView === 'campaigns' ? 'bg-secondary-container text-on-secondary-container font-semibold shadow-sm' : 'text-on-surface-variant hover:bg-surface-container-low'}"
      >
        <span class="material-symbols-outlined" data-icon="rocket_launch">rocket_launch</span>
        <span class="text-body-md">Campaign Wizard</span>
      </button>
    </nav>
    <div class="p-4 border-t border-outline-variant bg-surface-container-low/30">
      <span class="text-label-xs font-label-xs text-outline font-semibold">v1.1.0 • Client Admin</span>
    </div>
  </aside>

  <!-- Main Canvas Frame -->
  <main class="flex-grow pt-24 pb-28 md:pb-12 md:pl-72 px-6 max-w-7xl mx-auto w-full transition-all duration-200">

    <!-- Conditional View Rendering -->
    {#if currentView === 'dashboard'}
      <AccountDashboard
        {accounts}
        on:onboardRequest={() => currentView = 'onboard'}
        on:updateAccounts={handleUpdateAccounts}
      />
    {:else if currentView === 'onboard'}
      <AccountOnboardForm
        on:onboard={handleOnboarded}
        on:cancel={() => currentView = 'dashboard'}
      />
    {:else if currentView === 'campaigns'}
      <!-- EXISTING CAMPAIGN SETUP WIZARD VIEW -->
      <div class="max-w-4xl mx-auto space-y-6">
        <div>
          <h2 class="text-display-lg-mobile md:text-display-lg font-display-lg text-on-surface">Campaign Setup</h2>
          <p class="text-body-md text-on-surface-variant">Configure outbound messages, load lead batches, and trigger campaigns.</p>
        </div>

        <!-- Progress Stepper -->
        <nav aria-label="Progress Stepper" class="py-2">
          <div class="flex justify-between items-center relative py-4">
            <div class="absolute h-[2px] bg-outline-variant w-full top-1/2 -translate-y-1/2 z-0"></div>
            <div
              class="absolute h-[2px] bg-primary top-1/2 -translate-y-1/2 z-0 transition-all duration-500"
              style="width: {currentCampaignStep === 1 ? '16%' : currentCampaignStep === 2 ? '50%' : '100%'}">
            </div>

            <button
              on:click={() => goToStep(1)}
              type="button"
              class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
              aria-current={currentCampaignStep === 1 ? "step" : undefined}>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold transition-all duration-300
                {currentCampaignStep >= 1 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
                1
              </div>
              <span class="font-semibold text-label-sm text-primary">Basics</span>
            </button>

            <button
              on:click={() => goToStep(2)}
              type="button"
              class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
              aria-current={currentCampaignStep === 2 ? "step" : undefined}>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold transition-all duration-300
                {currentCampaignStep >= 2 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
                2
              </div>
              <span class="font-semibold text-label-sm text-primary">Audience</span>
            </button>

            <button
              on:click={() => goToStep(3)}
              type="button"
              class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
              aria-current={currentCampaignStep === 3 ? "step" : undefined}>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold transition-all duration-300
                {currentCampaignStep >= 3 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
                3
              </div>
              <span class="font-semibold text-label-sm text-primary">Content</span>
            </button>
          </div>
        </nav>

        <!-- STEP 1: CAMPAIGN BASICS -->
        {#if currentCampaignStep === 1}
          <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="basics-heading">
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <div class="flex items-center gap-2 mb-6">
                <span class="material-symbols-outlined text-primary">info</span>
                <h2 id="basics-heading" class="font-headline-md text-headline-md text-primary">Campaign Basics</h2>
              </div>

              <div class="space-y-6">
                <div class="flex flex-col gap-2">
                  <label class="font-label-md text-label-md text-on-surface-variant ml-1" for="campaign-name">Campaign Name</label>
                  <input
                    id="campaign-name"
                    bind:value={campaignName}
                    class="h-12 border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all placeholder:text-outline-variant bg-white"
                    placeholder="e.g. Summer Launch 2024"
                    type="text"
                  />
                </div>

                <div class="flex flex-col gap-2">
                  <label class="font-label-md text-label-md text-on-surface-variant ml-1" for="objective">Campaign Objective</label>
                  <div class="relative">
                    <select
                      id="objective"
                      bind:value={objective}
                      class="w-full h-12 appearance-none border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none bg-white pr-10"
                    >
                      <option value="awareness">Awareness & Brand Outreach</option>
                      <option value="conversion">High-Intent Lead Qualification</option>
                      <option value="retention">Customer Loyalty & Re-engagement</option>
                    </select>
                    <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">expand_more</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Channels -->
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <h2 class="font-label-md text-label-md text-on-surface-variant mb-4 ml-1">Delivery Channels</h2>
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">email</span>
                    <span class="font-body-lg text-body-lg text-primary">Email</span>
                  </div>
                  <input
                    type="checkbox"
                    bind:checked={deliveryChannels.email}
                    class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
                  />
                </label>

                <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">sms</span>
                    <span class="font-body-lg text-body-lg text-primary">SMS</span>
                  </div>
                  <input
                    type="checkbox"
                    bind:checked={deliveryChannels.sms}
                    class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
                  />
                </label>

                <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">notifications_active</span>
                    <span class="font-body-lg text-body-lg text-primary">Push</span>
                  </div>
                  <input
                    type="checkbox"
                    bind:checked={deliveryChannels.push}
                    class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
                  />
                </label>
              </div>
            </div>

            <!-- Schedule & Budget -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
                <h2 class="font-label-md text-label-md text-on-surface-variant mb-6 ml-1">Schedule Range</h2>
                <div class="flex flex-col gap-4">
                  <div class="relative">
                    <label for="start-date" class="absolute -top-2 left-3 bg-white px-1 font-label-sm text-label-sm text-primary">Start Date</label>
                    <div class="h-12 border border-primary border-2 rounded-lg flex items-center px-4 justify-between bg-white">
                      <input id="start-date" type="date" bind:value={startDate} class="w-full bg-transparent border-none outline-none font-body-md text-body-md focus:ring-0" />
                    </div>
                  </div>
                  <div class="relative mt-2">
                    <label for="end-date" class="absolute -top-2 left-3 bg-white px-1 font-label-sm text-label-sm text-on-surface-variant">End Date</label>
                    <div class="h-12 border border-outline-variant rounded-lg flex items-center px-4 justify-between bg-white">
                      <input id="end-date" type="date" bind:value={endDate} class="w-full bg-transparent border-none outline-none font-body-md text-body-md focus:ring-0" />
                    </div>
                  </div>
                </div>
              </div>

              <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
                <h2 class="font-label-md text-label-md text-on-surface-variant mb-6 ml-1">Campaign Budget</h2>
                <div class="relative">
                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant font-body-lg text-body-lg">$</span>
                  <input
                    class="w-full h-12 border border-outline-variant rounded-lg pl-8 pr-12 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all font-body-lg text-body-lg bg-white"
                    placeholder="0.00"
                    type="number"
                    bind:value={budget}
                  />
                  <div class="absolute right-3 top-1/2 -translate-y-1/2">
                    <span class="font-label-sm text-label-sm text-on-surface-variant font-semibold">USD</span>
                  </div>
                </div>
                <p class="mt-4 text-on-surface-variant font-label-sm text-label-sm flex items-center gap-1">
                  <span class="material-symbols-outlined text-[14px]">trending_up</span>
                  Recommended minimum: $500.00
                </p>
              </div>
            </div>
          </section>
        {/if}

        <!-- STEP 2: AUDIENCE -->
        {#if currentCampaignStep === 2}
          <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="audience-heading">
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <div class="flex items-center gap-2 mb-6">
                <span class="material-symbols-outlined text-primary">groups</span>
                <h2 id="audience-heading" class="font-headline-md text-headline-md text-primary">Target Audience Ingestion</h2>
              </div>

              <div class="space-y-4 mb-6">
                <div class="flex justify-between items-center">
                  <label class="font-label-md text-label-md text-on-surface-variant" for="target-list-select">Choose Target List</label>
                  <button
                    on:click={() => showCreateTargetList = !showCreateTargetList}
                    class="text-sm font-semibold text-secondary hover:underline flex items-center gap-1 focus:outline-none"
                  >
                    <span class="material-symbols-outlined text-xs">add</span> Create New List
                  </button>
                </div>

                {#if showCreateTargetList}
                  <div class="p-4 border border-outline-variant bg-surface-container-low rounded-lg flex gap-3 items-center animate-fadeIn">
                    <input
                      type="text"
                      placeholder="Target List Name (e.g. DeFi Whales)"
                      bind:value={newTargetListName}
                      class="flex-grow h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                    />
                    <button
                      on:click={handleCreateTargetList}
                      class="h-10 px-4 bg-primary text-on-primary rounded-lg font-label-md hover:bg-primary/95 transition-all text-xs"
                    >
                      Save
                    </button>
                  </div>
                {/if}

                <div class="relative">
                  <select
                    id="target-list-select"
                    bind:value={selectedTargetListId}
                    class="w-full h-12 appearance-none border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary bg-white pr-10"
                  >
                    {#each targetLists as list}
                      <option value={list.id}>{list.name}</option>
                    {/each}
                  </select>
                  <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">expand_more</span>
                </div>
              </div>
            </div>

            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <h3 class="font-label-md text-label-md text-on-surface-variant mb-3 ml-1">Import Leads via CSV / TXT</h3>
              <div
                on:dragover|preventDefault
                on:drop={handleCsvDrop}
                role="region"
                aria-label="File Upload Area"
                class="border-2 border-dashed border-outline-variant rounded-lg p-8 flex flex-col items-center justify-center bg-surface-container-low hover:bg-surface-container-low/80 hover:border-primary transition-all cursor-pointer relative"
              >
                <input
                  type="file"
                  accept=".csv,.txt"
                  on:change={handleCsvSelect}
                  class="absolute inset-0 opacity-0 cursor-pointer"
                  id="csv-file-input"
                />
                <span class="material-symbols-outlined text-outline text-4xl mb-2">upload_file</span>
                <p class="font-body-lg text-body-lg text-on-surface-variant text-center">
                  Drag and drop your <strong>.csv</strong> or <strong>.txt</strong> file here
                </p>
                <p class="text-xs text-outline mt-1 text-center">
                  Columns: username, phone_number, first_name, last_name, metadata
                </p>
              </div>

              {#if fileError}
                <div class="p-3 bg-error-container text-error rounded-lg text-xs mt-3 flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm">error</span>
                  {fileError}
                </div>
              {/if}

              {#if uploadedFile}
                <div class="p-3 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-lg text-xs mt-3 flex items-center justify-between">
                  <span>Uploaded: <strong>{uploadedFile.name}</strong></span>
                  <button on:click={() => {uploadedFile = null; importSuccessMessage = ""; parsedLeads = [];}} class="text-emerald-800 hover:underline">Remove</button>
                </div>
              {/if}
            </div>

            <!-- Manual entry -->
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <h3 class="font-label-md text-label-md text-on-surface-variant mb-3 ml-1">Add Lead Manually</h3>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <input
                  type="text"
                  placeholder="@username (Required)"
                  bind:value={manualLeadUsername}
                  class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                />
                <input
                  type="text"
                  placeholder="Phone number"
                  bind:value={manualLeadPhone}
                  class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                />
                <input
                  type="text"
                  placeholder="First Name"
                  bind:value={manualLeadFirstName}
                  class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                />
                <input
                  type="text"
                  placeholder="Last Name"
                  bind:value={manualLeadLastName}
                  class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                />
                <input
                  type="text"
                  placeholder="Metadata (JSON context)"
                  bind:value={manualLeadMetadata}
                  class="sm:col-span-2 h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                />
              </div>
              <button
                on:click={handleAddManualLead}
                class="mt-4 h-10 px-6 bg-secondary text-on-primary rounded-lg font-label-md hover:bg-secondary/95 transition-all w-full sm:w-auto"
              >
                Add to Batch
              </button>
            </div>

            {#if parsedLeads.length > 0}
              <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm animate-fadeIn">
                <h3 class="font-label-md text-label-md text-on-surface-variant mb-3">Current Batch Preview ({parsedLeads.length} leads)</h3>
                <div class="overflow-x-auto max-h-40">
                  <table class="w-full text-left text-xs">
                    <thead>
                      <tr class="bg-surface-container border-b border-outline-variant">
                        <th class="p-2">Username</th>
                        <th class="p-2">First Name</th>
                        <th class="p-2">Phone</th>
                      </tr>
                    </thead>
                    <tbody>
                      {#each parsedLeads as lead}
                        <tr class="border-b border-outline-variant/50">
                          <td class="p-2 font-semibold text-primary">{lead.username}</td>
                          <td class="p-2">{lead.firstName || "—"}</td>
                          <td class="p-2">{lead.phoneNumber || "—"}</td>
                        </tr>
                      {/each}
                    </tbody>
                  </table>
                </div>
              </div>
            {/if}
          </section>
        {/if}

        <!-- STEP 3: CONTENT -->
        {#if currentCampaignStep === 3}
          <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="content-heading">
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <div class="flex items-center gap-2 mb-6">
                <span class="material-symbols-outlined text-primary">chat</span>
                <h2 id="content-heading" class="font-headline-md text-headline-md text-primary">Outreach Content & Spintax</h2>
              </div>

              <div class="space-y-6">
                <div class="flex flex-col gap-2">
                  <div class="flex justify-between items-center ml-1">
                    <label class="font-label-md text-label-md text-on-surface-variant" for="spintax-editor">Spintax Message Template</label>
                    <div class="flex gap-2">
                      <span class="text-xs text-on-surface-variant">Insert:</span>
                      <button on:click={() => insertVariable("first_name")} class="text-xs text-primary font-semibold hover:underline">{"{first_name}"}</button>
                      <button on:click={() => insertVariable("username")} class="text-xs text-primary font-semibold hover:underline">{"{username}"}</button>
                    </div>
                  </div>

                  <textarea
                    id="spintax-editor"
                    bind:value={spintaxTemplate}
                    class="w-full h-36 border border-outline-variant rounded-lg p-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all font-mono text-sm bg-white resize-none"
                    placeholder="Template syntax example: &#123;Hi|Hello&#125; &#123;username&#125;"
                  ></textarea>

                  {#if spintaxError}
                    <div class="p-3 bg-error-container text-error rounded-lg text-xs flex items-center gap-2">
                      <span class="material-symbols-outlined text-sm">warning</span>
                      {spintaxError}
                    </div>
                  {/if}
                </div>

                <div class="p-4 border border-outline-variant rounded-lg bg-surface-container-low flex items-start gap-4">
                  <input
                    type="checkbox"
                    bind:checked={useLlmPersonalization}
                    id="llm-toggle"
                    class="w-5 h-5 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer mt-0.5"
                  />
                  <div class="flex flex-col gap-1">
                    <label for="llm-toggle" class="font-body-lg text-body-lg font-semibold text-primary cursor-pointer">
                      AI-Powered First-Offer Personalization
                    </label>
                    <p class="text-xs text-on-surface-variant">
                      Uses an LLM to dynamically rewrite spintax outreach offer based on public bio or custom metadata.
                    </p>
                  </div>
                </div>

                <div class="flex flex-col gap-2">
                  <label class="font-label-md text-label-md text-on-surface-variant ml-1" for="campaign-status">Initial Launch Status</label>
                  <select
                    id="campaign-status"
                    bind:value={campaignStatus}
                    class="w-full h-12 appearance-none border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary bg-white outline-none"
                  >
                    <option value="DRAFT">Draft Mode (Save only)</option>
                    <option value="ACTIVE">Active (Launch immediately)</option>
                    <option value="PAUSED">Paused</option>
                  </select>
                </div>
              </div>
            </div>

            {#if saveSuccess}
              <div class="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex gap-3 items-center animate-fadeIn" role="alert">
                <span class="material-symbols-outlined text-emerald-600">verified</span>
                <div>
                  <h4 class="font-bold text-sm">Campaign Saved Successfully!</h4>
                  <p class="text-xs">Your campaign configurations have been persisted.</p>
                </div>
              </div>
            {/if}
          </section>
        {/if}

        <!-- Wizard Persistent Action Footer (rendered within main panel for Campaigns) -->
        <footer class="mt-8 flex gap-4 max-w-lg mx-auto">
          {#if currentCampaignStep > 1}
            <button
              on:click={handleBack}
              class="flex-1 h-12 bg-white border border-outline-variant rounded-lg text-on-surface-variant font-semibold hover:bg-surface-container-low transition-colors active:scale-95 flex items-center justify-center gap-1"
            >
              <span class="material-symbols-outlined text-[18px]">arrow_back</span>
              Back
            </button>
          {/if}

          <button
            on:click={handleNext}
            disabled={isSaving}
            class="flex-[2] h-12 bg-primary text-on-primary rounded-lg font-bold shadow-md hover:opacity-95 transition-all flex items-center justify-center gap-2 active:scale-95 disabled:opacity-50"
          >
            {#if isSaving}
              <span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
              Processing...
            {:else if currentCampaignStep < 3}
              Continue
              <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
            {:else}
              Save & Launch Campaign
              <span class="material-symbols-outlined text-[18px]">rocket_launch</span>
            {/if}
          </button>
        </footer>
      </div>
    {/if}

        </div>

  <!-- Bottom Navigation Bar (Mobile Only) -->
  <nav class="md:hidden fixed bottom-0 left-0 w-full flex justify-around items-center bg-white border-t border-outline-variant shadow-lg z-40 h-16 transition-all" aria-label="Mobile Navigation">
    <button
      on:click={() => currentView = 'dashboard'}
      class="flex flex-col items-center justify-center flex-grow py-1 focus:outline-none focus:text-primary
        {currentView === 'dashboard' || currentView === 'onboard' ? 'text-secondary' : 'text-on-surface-variant'}"
    >
      <span class="material-symbols-outlined">analytics</span>
      <span class="text-[10px] font-bold">Accounts</span>
    </button>
    <button
      on:click={() => currentView = 'campaigns'}
      class="flex flex-col items-center justify-center flex-grow py-1 focus:outline-none focus:text-primary
        {currentView === 'campaigns' ? 'text-secondary' : 'text-on-surface-variant'}"
    >
      <span class="material-symbols-outlined">rocket_launch</span>
      <span class="text-[10px] font-bold">Campaigns</span>
    </button>
  </nav>

</div>

<style>
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fadeIn {
    animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
</style>
