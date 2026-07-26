<script>
  import AIPromptEditor from "./lib/AIPromptEditor.svelte";

  // Navigation State
  let activeTab = "accounts"; // "accounts", "campaigns", or "aiPersona"
  $: pageTitle = activeTab === "accounts"
    ? "Accounts Center"
    : activeTab === "campaigns"
      ? "Campaign Setup"
      : "AI Persona";

  // STATE FOR CAMPAIGN SETUP (Tab: "campaigns")
  let currentStep = 1; // 1: Basics, 2: Audience, 3: Content

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
    if (currentStep < 3) {
      currentStep += 1;
    } else {
      handleSaveCampaign();
    }
  }

  // Go to step directly
  function goToStep(step) {
    currentStep = step;
  }

  function handleBack() {
    if (currentStep > 1) {
      currentStep -= 1;
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

<div class="min-h-screen flex flex-col md:flex-row font-body-md text-body-md bg-surface text-on-surface">

  <!-- Sidebar Navigation (Desktop) -->
  <aside class="fixed inset-y-0 left-0 z-50 hidden md:flex flex-col py-6 h-full w-72 bg-surface border-r border-outline-variant">
    <div class="px-6 mb-8">
      <span class="font-headline-md text-headline-md font-black text-secondary select-none">Admin Center</span>
    </div>

    <!-- User Profile Header -->
    <div class="px-6 mb-8 flex items-center gap-3">
      <div class="w-12 h-12 rounded-xl bg-surface-container-high overflow-hidden">
        <img class="w-full h-full object-cover" alt="Administrator Headshot" src="https://lh3.googleusercontent.com/aida-public/AB6AXuD239slOGd9LYv8iDfJzig1jytZ3HgInV8SsUJHTyGJ7I_s0R5BhChP54dgkrxo8CRH_6B0YhQMmMYXS-dsKiEfHiGLn5YrF6qx2D4o9TN6O1ShunqwjrOOevqwpBBq1_41swxJdCbOH8n9h8hc9KxJpfPYBHgn6bIKcd5GVqT8TgxGFrzxqKRaBe_qgNBaKyUauhgFh_m4SXR9JrubsQth0M7XjuA3RiMSWXaGPEHXYSiV1gC_JqBICzjcrSAWi0sQBRR9uw5KKks"/>
      </div>
      <div>
        <p class="font-label-md text-label-md font-bold text-primary">Admin User</p>
        <p class="text-xs text-on-surface-variant">System Administrator</p>
      </div>
    </div>

    <!-- Navigation Links -->
    <nav class="flex-1 space-y-1">
      <button
        on:click={() => activeTab = "accounts"}
        class="w-[calc(100%-16px)] flex items-center gap-3 rounded-full mx-2 px-4 py-3 text-left transition-all duration-300
          {activeTab === 'accounts' ? 'bg-primary text-on-primary font-bold' : 'text-on-surface-variant font-medium hover:bg-surface-container-high'}"
      >
        <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {activeTab === 'accounts' ? 1 : 0};">manage_accounts</span>
        <span class="font-label-md text-label-md">Account Overview</span>
      </button>

      <button
        on:click={() => activeTab = "campaigns"}
        class="w-[calc(100%-16px)] flex items-center gap-3 rounded-full mx-2 px-4 py-3 text-left transition-all duration-300
          {activeTab === 'campaigns' ? 'bg-primary text-on-primary font-bold' : 'text-on-surface-variant font-medium hover:bg-surface-container-high'}"
      >
        <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {activeTab === 'campaigns' ? 1 : 0};">dashboard</span>
        <span class="font-label-md text-label-md">Campaign Setup</span>
      </button>

      <button
        on:click={() => activeTab = "aiPersona"}
        class="w-[calc(100%-16px)] flex items-center gap-3 rounded-full mx-2 px-4 py-3 text-left transition-all duration-300
          {activeTab === 'aiPersona' ? 'bg-primary text-on-primary font-bold' : 'text-on-surface-variant font-medium hover:bg-surface-container-high'}"
      >
        <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {activeTab === 'aiPersona' ? 1 : 0};">psychology</span>
        <span class="font-label-md text-label-md">AI Persona</span>
      </button>

      <button
        class="w-[calc(100%-16px)] flex items-center gap-3 rounded-full mx-2 px-4 py-3 text-left text-on-surface-variant font-medium hover:bg-surface-container-high transition-all duration-300"
      >
        <span class="material-symbols-outlined">monitor_heart</span>
        <span class="font-label-md text-label-md">Health Metrics</span>
      </button>

      <button
        class="w-[calc(100%-16px)] flex items-center gap-3 rounded-full mx-2 px-4 py-3 text-left text-on-surface-variant font-medium hover:bg-surface-container-high transition-all duration-300"
      >
        <span class="material-symbols-outlined">tune</span>
        <span class="font-label-md text-label-md">Global Settings</span>
      </button>
    </nav>

    <!-- Operational System Status -->
    <div class="px-6 mt-auto">
      <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant">
        <p class="font-label-sm text-label-sm text-on-surface-variant mb-2">System Status</p>
        <div class="flex items-center gap-2">
          <div class="w-2 h-2 rounded-full bg-green-500"></div>
          <span class="text-xs font-medium">All Services Operational</span>
        </div>
      </div>
    </div>
  </aside>

  <!-- Mobile Top Header App Bar -->
  <header class="fixed top-0 left-0 w-full z-40 flex justify-between items-center px-margin-mobile h-16 bg-surface border-b border-outline-variant md:left-72 md:w-[calc(100%-288px)]">
    <div class="flex items-center gap-4">
      <button class="md:hidden p-2 hover:bg-surface-container-low transition-colors active:opacity-80" aria-label="Menu">
        <span class="material-symbols-outlined text-primary">menu</span>
      </button>
      <h1 class="font-headline-sm text-headline-sm font-bold text-primary select-none">
        {pageTitle}
      </h1>
    </div>
    <div class="flex items-center gap-2">
      <button class="p-2 hover:bg-surface-container-low transition-colors active:opacity-80" aria-label="Search">
        <span class="material-symbols-outlined text-primary">search</span>
      </button>
      <div class="hidden md:flex items-center gap-4 px-4">
        <span class="font-label-md text-label-md text-on-surface-variant">v2.4.0</span>
        <div class="w-8 h-8 rounded-full bg-secondary-container flex items-center justify-center text-on-secondary-container font-bold text-xs select-none">AU</div>
      </div>
    </div>
  </header>

  <!-- Main Scrollable Canvas Area -->
  <main class="flex-grow pt-20 pb-28 md:pb-8 md:pl-72 w-full min-h-screen">
    <div class="px-margin-mobile md:px-margin-desktop max-w-6xl mx-auto w-full">

      <!-- ==================== VIEW 1: ACCOUNT MANAGEMENT DASHBOARD ==================== -->
      {#if activeTab === "accounts"}
        <section class="animate-fadeIn mt-4" aria-labelledby="accounts-heading">
          <!-- Heading Section -->
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <div>
              <h2 id="accounts-heading" class="font-headline-lg text-headline-lg text-primary font-black">Telegram Accounts</h2>
              <p class="font-body-md text-body-md text-on-surface-variant">Onboard, configure, and monitor outreach campaign sessions.</p>
            </div>
            <div class="flex gap-3">
              <button
                on:click={openOnboardModal}
                class="px-5 py-2.5 bg-secondary text-on-secondary font-bold text-label-md rounded-xl shadow-md shadow-secondary/20 hover:bg-secondary/90 transition-all flex items-center gap-2"
              >
                <span class="material-symbols-outlined text-[20px]">person_add</span>
                Onboard Account
              </button>
            </div>
          </div>

          <!-- Live Search & Filtering Panel -->
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-4 mb-8 shadow-sm flex flex-col gap-4">
            <div class="flex flex-col md:flex-row gap-4 items-center">
              <!-- Search box -->
              <div class="relative w-full md:flex-grow">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
                <input
                  bind:value={searchQuery}
                  class="w-full pl-10 pr-4 py-2.5 bg-surface border border-outline-variant rounded-xl focus:ring-2 focus:ring-secondary focus:border-secondary outline-none transition-all text-sm"
                  placeholder="Search by username, phone number or client company..."
                  type="text"
                />
              </div>

              <!-- Filter chips status container -->
              <div class="w-full md:w-auto flex flex-wrap gap-2 items-center">
                <span class="text-xs font-bold text-on-surface-variant uppercase tracking-wider mr-2 select-none">Status:</span>
                <button
                  on:click={() => selectedFilter = "all"}
                  class="px-4 py-1.5 rounded-full font-label-md text-xs whitespace-nowrap transition-all duration-200
                    {selectedFilter === 'all' ? 'bg-secondary text-on-secondary shadow-sm font-semibold' : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'}"
                >
                  All
                </button>
                <button
                  on:click={() => selectedFilter = "ACTIVE"}
                  class="px-4 py-1.5 rounded-full font-label-md text-xs whitespace-nowrap transition-all duration-200
                    {selectedFilter === 'ACTIVE' ? 'bg-secondary text-on-secondary shadow-sm font-semibold' : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'}"
                >
                  Active
                </button>
                <button
                  on:click={() => selectedFilter = "TEMPORARY_SPAM_BLOCK"}
                  class="px-4 py-1.5 rounded-full font-label-md text-xs whitespace-nowrap transition-all duration-200
                    {selectedFilter === 'TEMPORARY_SPAM_BLOCK' ? 'bg-secondary text-on-secondary shadow-sm font-semibold' : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'}"
                >
                  Spam-Block
                </button>
                <button
                  on:click={() => selectedFilter = "RE_AUTHORIZATION_REQUIRED"}
                  class="px-4 py-1.5 rounded-full font-label-md text-xs whitespace-nowrap transition-all duration-200
                    {selectedFilter === 'RE_AUTHORIZATION_REQUIRED' ? 'bg-secondary text-on-secondary shadow-sm font-semibold' : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'}"
                >
                  Pending/Re-auth
                </button>
                <button
                  on:click={() => selectedFilter = "PERMANENT_BAN"}
                  class="px-4 py-1.5 rounded-full font-label-md text-xs whitespace-nowrap transition-all duration-200
                    {selectedFilter === 'PERMANENT_BAN' ? 'bg-secondary text-on-secondary shadow-sm font-semibold' : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'}"
                >
                  Banned
                </button>
              </div>
            </div>
          </div>

          <!-- Accounts Grid -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {#each filteredAccounts as acc (acc.id)}
              <div class="bg-surface-container-lowest border border-outline-variant rounded-2xl p-6 transition-all duration-300 hover:shadow-lg hover:-translate-y-1 relative flex flex-col justify-between">
                <div>
                  <!-- Card Header Status Badge -->
                  <div class="flex justify-between items-start mb-4">
                    <div class="w-12 h-12 rounded-xl bg-surface-container-high flex items-center justify-center">
                      <span class="material-symbols-outlined text-secondary">corporate_fare</span>
                    </div>

                    <!-- Health status labels with exact color-coding -->
                    {#if acc.status === "ACTIVE"}
                      <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-green-50 text-green-700 text-[10px] font-bold uppercase tracking-wider">
                        <span class="w-2 h-2 rounded-full bg-green-500"></span>
                        Active / Healthy
                      </span>
                    {:else if acc.status === "TEMPORARY_SPAM_BLOCK"}
                      <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-orange-50 text-orange-700 text-[10px] font-bold uppercase tracking-wider">
                        <span class="w-2 h-2 rounded-full bg-orange-500"></span>
                        Spam-Block (At Risk)
                      </span>
                    {:else if acc.status === "RE_AUTHORIZATION_REQUIRED"}
                      <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-blue-50 text-blue-700 text-[10px] font-bold uppercase tracking-wider">
                        <span class="w-2 h-2 rounded-full bg-blue-500"></span>
                        Re-Auth Required
                      </span>
                    {:else if acc.status === "PERMANENT_BAN"}
                      <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-red-50 text-red-700 text-[10px] font-bold uppercase tracking-wider">
                        <span class="w-2 h-2 rounded-full bg-red-500"></span>
                        Permanent Ban
                      </span>
                    {/if}
                  </div>

                  <!-- Username and Details -->
                  <h3 class="font-headline-sm text-headline-sm text-primary font-bold mb-1">@{acc.username}</h3>
                  <p class="text-xs text-on-surface-variant font-medium mb-4">{acc.company} • {acc.phoneNumber}</p>

                  <!-- Divider and Grid Stats -->
                  <div class="grid grid-cols-2 gap-4 py-4 border-t border-b border-surface-variant my-4">
                    <div>
                      <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-wider mb-0.5">Assigned Proxy</p>
                      <p class="font-label-md text-xs font-semibold text-primary truncate max-w-full" title={acc.proxy}>
                        {acc.proxy !== "None" ? acc.proxy.split('://')[1] || acc.proxy : "None Bound"}
                      </p>
                    </div>
                    <div>
                      <p class="text-[10px] text-on-surface-variant font-bold uppercase tracking-wider mb-0.5">Trust Score / Limit</p>
                      <p class="font-label-md text-xs font-semibold text-primary">
                        {acc.currentTrustScore ? acc.currentTrustScore + " / 10" : "N/A"} ({acc.dailyLimit} msg)
                      </p>
                    </div>
                  </div>
                </div>

                <div class="mt-4 flex gap-2">
                  <button class="flex-1 py-2 bg-surface border border-outline-variant text-secondary font-bold text-xs rounded-xl hover:bg-secondary-container hover:text-on-secondary-container transition-colors active:scale-[0.98]">
                    View Details
                  </button>
                  <button class="px-3 py-2 bg-surface border border-outline-variant text-error font-bold text-xs rounded-xl hover:bg-error-container/20 transition-colors active:scale-[0.98]" aria-label="Disconnect">
                    <span class="material-symbols-outlined text-[18px]">power_off</span>
                  </button>
                </div>
              </div>
            {/each}

            <!-- Onboarding Grid Box Placeholder -->
            <button
              on:click={openOnboardModal}
              class="border-2 border-dashed border-outline-variant hover:border-secondary rounded-2xl p-6 flex flex-col items-center justify-center text-center bg-surface-container-low/30 hover:bg-surface-container-low transition-all cursor-pointer group"
            >
              <div class="w-16 h-16 rounded-full bg-surface-container flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                <span class="material-symbols-outlined text-outline scale-125">add</span>
              </div>
              <p class="font-headline-sm text-headline-sm text-primary font-bold">Onboard New Session</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant px-8 mt-1">Add Telegram account via OTP verification or session files upload.</p>
            </button>
          </div>
        </section>
      {/if}

      <!-- ==================== VIEW 2: CAMPAIGN SETUP WIZARD ==================== -->
      {#if activeTab === "campaigns"}
        <!-- Multi-Step Progress Stepper -->
        <nav aria-label="Progress Stepper" class="mt-4 mb-8">
          <div class="flex justify-between items-center relative py-4">
            <div class="absolute h-[2px] bg-outline-variant w-full top-1/2 -translate-y-1/2 z-0"></div>
            <div
              class="absolute h-[2px] bg-primary top-1/2 -translate-y-1/2 z-0 transition-all duration-500"
              style="width: {currentStep === 1 ? '16%' : currentStep === 2 ? '50%' : '100%'}">
            </div>

            <!-- Step buttons -->
            <button
              on:click={() => goToStep(1)}
              type="button"
              class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
              aria-current={currentStep === 1 ? "step" : undefined}>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-label-md transition-all duration-300
                {currentStep >= 1 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
                1
              </div>
              <span class="font-label-sm text-label-sm {currentStep >= 1 ? 'text-primary font-semibold' : 'text-on-surface-variant'}">Basics</span>
            </button>

            <button
              on:click={() => goToStep(2)}
              type="button"
              class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
              aria-current={currentStep === 2 ? "step" : undefined}>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-label-md transition-all duration-300
                {currentStep >= 2 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
                2
              </div>
              <span class="font-label-sm text-label-sm {currentStep >= 2 ? 'text-primary font-semibold' : 'text-on-surface-variant'}">Audience</span>
            </button>

            <button
              on:click={() => goToStep(3)}
              type="button"
              class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
              aria-current={currentStep === 3 ? "step" : undefined}>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-label-md transition-all duration-300
                {currentStep >= 3 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
                3
              </div>
              <span class="font-label-sm text-label-sm {currentStep >= 3 ? 'text-primary font-semibold' : 'text-on-surface-variant'}">Content</span>
            </button>
          </div>
        </nav>

        <!-- STEP 1: CAMPAIGN BASICS -->
        {#if currentStep === 1}
          <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="basics-heading">
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <div class="flex items-center gap-2 mb-6">
                <span class="material-symbols-outlined text-primary">info</span>
                <h2 id="basics-heading" class="font-headline-md text-headline-md text-primary">Campaign Basics</h2>
              </div>

              <div class="space-y-6">
                <!-- Campaign Name -->
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

                <!-- Objective Selection -->
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

            <!-- Channels Toggle Section -->
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <h2 class="font-label-md text-label-md text-on-surface-variant mb-4 ml-1">Delivery Channels</h2>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">email</span>
                    <span class="font-body-lg text-body-lg">Email</span>
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
                    <span class="font-body-lg text-body-lg">SMS</span>
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
                    <span class="font-body-lg text-body-lg">Push</span>
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
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
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
        {#if currentStep === 2}
          <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="audience-heading">
            <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
              <div class="flex items-center gap-2 mb-6">
                <span class="material-symbols-outlined text-primary">groups</span>
                <h2 id="audience-heading" class="font-headline-md text-headline-md text-primary">Target Audience Configuration</h2>
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

            <!-- Ingestion area -->
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
              </div>

              {#if fileError}
                <div class="p-3 bg-error-container text-error rounded-lg text-xs mt-3 flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm">error</span>
                  {fileError}
                </div>
              {/if}

              {#if uploadedFile}
                <div class="p-3 bg-green-50 text-green-700 border border-green-200 rounded-lg text-xs mt-3 flex items-center justify-between">
                  <span>Uploaded: <strong>{uploadedFile.name}</strong></span>
                </div>
              {/if}
            </div>
          </section>
        {/if}

        <!-- STEP 3: CONTENT -->
        {#if currentStep === 3}
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
                  </div>
                  <textarea
                    id="spintax-editor"
                    bind:value={spintaxTemplate}
                    class="w-full h-36 border border-outline-variant rounded-lg p-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all font-mono text-sm bg-white resize-none"
                  ></textarea>
                </div>

                <!-- LLM personalizer toggle option -->
                <div class="p-4 border border-outline-variant rounded-lg bg-surface-container-low flex items-start gap-4 hover:border-primary transition-all">
                  <input
                    type="checkbox"
                    bind:checked={useLlmPersonalization}
                    id="llm-toggle"
                    class="w-5 h-5 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer"
                  />
                  <div>
                    <label for="llm-toggle" class="font-body-lg text-body-lg font-semibold text-primary cursor-pointer">
                      AI-Powered First-Offer Personalization
                    </label>
                  </div>
                </div>
              </div>
            </div>

            {#if saveSuccess}
              <div class="p-4 bg-green-50 border border-green-200 text-green-800 rounded-xl flex gap-3 items-center animate-fadeIn" role="alert">
                <span class="material-symbols-outlined text-green-600">verified</span>
                <div>
                  <h4 class="font-bold text-sm">Campaign Saved Successfully!</h4>
                </div>
              </div>
            {/if}
          </section>
        {/if}

        <!-- Footer actions for setup wizard -->
        <footer class="mt-8 flex gap-4 max-w-lg mx-auto w-full">
          {#if currentStep > 1}
            <button
              on:click={handleBack}
              class="flex-1 h-12 bg-white border border-outline-variant rounded-lg text-on-surface-variant font-label-md hover:bg-surface-container-low transition-colors active:scale-95 flex items-center justify-center gap-1"
            >
              <span class="material-symbols-outlined text-[18px]">arrow_back</span>
              Back
            </button>
          {/if}

          <button
            on:click={handleNext}
            disabled={isSaving}
            class="flex-[2] h-12 bg-primary text-on-primary rounded-lg font-label-md shadow-md hover:bg-primary/95 transition-all flex items-center justify-center gap-2 active:scale-95 disabled:opacity-50"
          >
            {#if isSaving}
              Processing...
            {:else if currentStep < 3}
              Continue
              <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
            {:else}
              Save Campaign
              <span class="material-symbols-outlined text-[18px]">rocket_launch</span>
            {/if}
          </button>
        </footer>
      {/if}

      {#if activeTab === "aiPersona"}
        <section class="animate-fadeIn mt-4" aria-labelledby="ai-persona-heading">
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <div>
              <h2 id="ai-persona-heading" class="font-headline-lg text-headline-lg text-primary font-black">AI Persona</h2>
              <p class="font-body-md text-body-md text-on-surface-variant">Tune system prompts, dynamic variables, and generation parameters for campaign conversations.</p>
            </div>
          </div>

          <AIPromptEditor />
        </section>
      {/if}

    </div>
  </main>

  <!-- Persistent Mobile Bottom Navigation Bar (Mobile Only) -->
  <nav class="fixed bottom-0 left-0 w-full z-40 flex justify-around items-center px-2 py-3 pb-safe bg-surface border-t border-outline-variant rounded-t-xl shadow-lg md:hidden">
    <button
      on:click={() => activeTab = "accounts"}
      class="flex flex-col items-center justify-center rounded-full px-4 py-1.5 transition-transform duration-150
        {activeTab === 'accounts' ? 'bg-primary/10 text-primary scale-105' : 'text-on-surface-variant'}"
      aria-label="Accounts View"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {activeTab === 'accounts' ? 1 : 0};">manage_accounts</span>
      <span class="font-label-sm text-[10px] font-semibold mt-0.5">Accounts</span>
    </button>

    <button
      on:click={() => activeTab = "campaigns"}
      class="flex flex-col items-center justify-center rounded-full px-4 py-1.5 transition-transform duration-150
        {activeTab === 'campaigns' ? 'bg-primary/10 text-primary scale-105' : 'text-on-surface-variant'}"
      aria-label="Campaign Setup View"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {activeTab === 'campaigns' ? 1 : 0};">dashboard</span>
      <span class="font-label-sm text-[10px] font-semibold mt-0.5">Campaigns</span>
    </button>

    <button
      on:click={() => activeTab = "aiPersona"}
      class="flex flex-col items-center justify-center rounded-full px-4 py-1.5 transition-transform duration-150
        {activeTab === 'aiPersona' ? 'bg-primary/10 text-primary scale-105' : 'text-on-surface-variant'}"
      aria-label="AI Persona View"
    >
      <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' {activeTab === 'aiPersona' ? 1 : 0};">psychology</span>
      <span class="font-label-sm text-[10px] font-semibold mt-0.5">Persona</span>
    </button>
  </nav>

  <!-- ==================== ONBOARDING MODAL ==================== -->
  {#if showOnboardModal}
    <div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div class="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl overflow-hidden border border-outline-variant flex flex-col max-h-[90vh]">

        <!-- Modal Header -->
        <header class="px-6 py-4 border-b border-outline-variant flex justify-between items-center bg-surface-container-low">
          <div>
            <h3 id="modal-title" class="font-headline-sm text-lg font-bold text-primary">Onboard Telegram Account</h3>
            <p class="text-xs text-on-surface-variant">Verify and bind a Telegram account to active proxies.</p>
          </div>
          <button on:click={closeOnboardModal} class="p-1 hover:bg-surface-container-high rounded-full transition-colors" aria-label="Close modal">
            <span class="material-symbols-outlined text-primary">close</span>
          </button>
        </header>

        <!-- Modal Body (Scrollable) -->
        <div class="p-6 overflow-y-auto flex-grow space-y-6">

          <!-- Tab selector for onboarding method -->
          <div class="flex border-b border-outline-variant">
            <button
              on:click={() => onboardMethod = "otp"}
              class="flex-1 py-3 font-semibold text-sm border-b-2 transition-all
                {onboardMethod === 'otp' ? 'border-secondary text-secondary font-bold' : 'border-transparent text-on-surface-variant hover:text-primary'}"
            >
              OTP Verification Code
            </button>
            <button
              on:click={() => onboardMethod = "file"}
              class="flex-1 py-3 font-semibold text-sm border-b-2 transition-all
                {onboardMethod === 'file' ? 'border-secondary text-secondary font-bold' : 'border-transparent text-on-surface-variant hover:text-primary'}"
            >
              Upload Session Files (.session / tdata)
            </button>
          </div>

          <!-- Proxy Settings Sub-panel -->
          <div class="p-4 bg-surface-container-low rounded-xl border border-outline-variant">
            <div class="flex items-center justify-between mb-3">
              <label class="font-label-md text-sm font-bold text-primary flex items-center gap-2 cursor-pointer" for="use-proxy-checkbox">
                <span class="material-symbols-outlined text-secondary">shield</span>
                Isolated Proxy Binding (SOCKS5/HTTP)
              </label>
              <input
                type="checkbox"
                id="use-proxy-checkbox"
                bind:checked={useProxy}
                class="w-5 h-5 rounded text-secondary focus:ring-secondary cursor-pointer"
              />
            </div>

            {#if useProxy}
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 animate-fadeIn">
                <!-- Protocol selection -->
                <div class="flex flex-col gap-1">
                  <label for="proxy-protocol" class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Protocol</label>
                  <select
                    id="proxy-protocol"
                    bind:value={proxyProtocol}
                    class="h-10 border border-outline-variant rounded-lg px-2 bg-white text-xs"
                  >
                    <option value="SOCKS5">SOCKS5</option>
                    <option value="HTTP">HTTP</option>
                  </select>
                </div>
                <!-- Host -->
                <div class="flex flex-col gap-1 sm:col-span-2">
                  <label for="proxy-host" class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Host/IP</label>
                  <input
                    type="text"
                    id="proxy-host"
                    placeholder="e.g. 185.112.44.12"
                    bind:value={proxyHost}
                    class="h-10 border border-outline-variant rounded-lg px-3 bg-white text-xs"
                  />
                </div>
                <!-- Port -->
                <div class="flex flex-col gap-1">
                  <label for="proxy-port" class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Port</label>
                  <input
                    type="text"
                    id="proxy-port"
                    placeholder="e.g. 1080"
                    bind:value={proxyPort}
                    class="h-10 border border-outline-variant rounded-lg px-3 bg-white text-xs"
                  />
                </div>
                <!-- Username -->
                <div class="flex flex-col gap-1">
                  <label for="proxy-user" class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Proxy User</label>
                  <input
                    type="text"
                    id="proxy-user"
                    placeholder="Optional"
                    bind:value={proxyUser}
                    class="h-10 border border-outline-variant rounded-lg px-3 bg-white text-xs"
                  />
                </div>
                <!-- Password -->
                <div class="flex flex-col gap-1">
                  <label for="proxy-pass" class="text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">Proxy Password</label>
                  <input
                    type="password"
                    id="proxy-pass"
                    placeholder="Optional"
                    bind:value={proxyPass}
                    class="h-10 border border-outline-variant rounded-lg px-3 bg-white text-xs"
                  />
                </div>
              </div>
            {/if}
          </div>

          <!-- Method 1: OTP Verification -->
          {#if onboardMethod === "otp"}
            <div class="space-y-4 animate-fadeIn">
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                  <label for="onboard-phone" class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Phone Number (International)</label>
                  <input
                    type="tel"
                    id="onboard-phone"
                    placeholder="e.g. +12025550143"
                    bind:value={onboardPhone}
                    disabled={otpSent}
                    class="h-11 border border-outline-variant rounded-lg px-3 bg-white text-sm"
                  />
                </div>
                <div class="flex flex-col gap-1">
                  <label for="onboard-username" class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Desired Username (Optional)</label>
                  <input
                    type="text"
                    id="onboard-username"
                    placeholder="e.g. outreach_pro"
                    bind:value={onboardUsername}
                    disabled={otpSent}
                    class="h-11 border border-outline-variant rounded-lg px-3 bg-white text-sm"
                  />
                </div>
              </div>

              <!-- Send OTP Trigger -->
              {#if !otpSent}
                <button
                  on:click={handleSendOtp}
                  disabled={otpSending}
                  class="w-full h-11 bg-primary text-on-primary font-bold text-sm rounded-lg hover:bg-primary/95 transition-all flex items-center justify-center gap-2"
                >
                  {#if otpSending}
                    Requesting OTP...
                  {:else}
                    Send Verification OTP Code
                  {/if}
                </button>
              {:else}
                <!-- OTP Input Area -->
                <div class="p-4 bg-green-50 border border-green-200 rounded-xl space-y-3 animate-fadeIn">
                  <p class="text-xs text-green-800 font-medium flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px]">sms</span>
                    OTP sent to {onboardPhone}. Check your Telegram active sessions.
                  </p>
                  <div class="flex flex-col gap-1">
                    <label for="onboard-otp" class="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Enter OTP Code</label>
                    <input
                      type="text"
                      id="onboard-otp"
                      placeholder="e.g. 48392"
                      bind:value={onboardOtp}
                      class="h-11 border border-outline-variant rounded-lg px-3 bg-white text-sm font-mono tracking-widest text-center"
                    />
                  </div>
                  <button
                    on:click={handleVerifyOtp}
                    disabled={otpVerifying}
                    class="w-full h-11 bg-secondary text-on-secondary font-bold text-sm rounded-lg hover:bg-secondary/95 transition-all flex items-center justify-center gap-2"
                  >
                    {#if otpVerifying}
                      Verifying session...
                    {:else}
                      Verify and Activate Session
                    {/if}
                  </button>
                </div>
              {/if}
            </div>
          {/if}

          <!-- Method 2: File Upload (.session/tdata) -->
          {#if onboardMethod === "file"}
            <div class="space-y-4 animate-fadeIn">
              <div
                on:dragover|preventDefault
                on:drop={handleSessionDrop}
                role="region"
                aria-label="Session File Dropper"
                class="border-2 border-dashed border-outline-variant rounded-2xl p-8 flex flex-col items-center justify-center bg-surface-container-low hover:bg-surface-container-low/80 hover:border-secondary transition-all cursor-pointer relative"
              >
                <input
                  type="file"
                  multiple
                  accept=".session"
                  on:change={handleSessionSelect}
                  class="absolute inset-0 opacity-0 cursor-pointer"
                  id="session-file-input"
                />
                <span class="material-symbols-outlined text-outline text-5xl mb-3">folder_zip</span>
                <p class="font-body-lg text-body-lg text-on-surface-variant text-center font-semibold">
                  Drag and drop .session / tdata files here
                </p>
                <p class="text-xs text-outline mt-1 text-center">
                  Supports bulk onboarding of multi-format accounts.
                </p>
              </div>

              {#if sessionFileError}
                <div class="p-3 bg-error-container text-error rounded-lg text-xs flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm">error</span>
                  {sessionFileError}
                </div>
              {/if}

              {#if uploadSuccessMessage}
                <div class="p-3 bg-green-50 text-green-700 border border-green-200 rounded-lg text-xs flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm">check_circle</span>
                  {uploadSuccessMessage}
                </div>
              {/if}
            </div>
          {/if}

          <!-- General error messages -->
          {#if onboardError}
            <div class="p-3 bg-error-container text-error rounded-lg text-xs flex items-center gap-2 animate-fadeIn">
              <span class="material-symbols-outlined text-sm">warning</span>
              {onboardError}
            </div>
          {/if}

          <!-- Onboarding Success Alert -->
          {#if onboardSuccess}
            <div class="p-4 bg-green-50 border border-green-200 text-green-800 rounded-xl flex gap-3 items-center animate-fadeIn" role="alert">
              <span class="material-symbols-outlined text-green-600 text-2xl">verified</span>
              <div>
                <h4 class="font-bold text-sm">Account Successfully Bound!</h4>
                <p class="text-xs">The Telegram session has been verified and isolated under proxy successfully.</p>
              </div>
            </div>
          {/if}

        </div>

        <!-- Modal Footer -->
        <footer class="px-6 py-4 border-t border-outline-variant bg-surface-container-low flex justify-end gap-3">
          <button on:click={closeOnboardModal} class="px-4 py-2 border border-outline-variant text-on-surface-variant bg-white font-bold text-xs rounded-xl hover:bg-surface-container-high transition-colors">
            Cancel
          </button>
        </footer>

      </div>
    </div>
  {/if}

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
