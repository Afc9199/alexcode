// Common UI Components for Modern Dashboard Design

const CommonUI = (() => {
	let currentUser = null;

	// Initialize common UI elements
	async function init() {
		await checkAuth();
		setupNavigation();
		setupSearch();
		setupLogout();
		updateUserBadge();
	}

	// Check authentication
	async function checkAuth() {
		try {
			const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
			if (!response.ok) {
				window.location.href = '/login.html';
				return;
			}
			currentUser = await response.json();
			return currentUser;
		} catch (error) {
			console.error('Auth check failed:', error);
			window.location.href = '/login.html';
			return null;
		}
	}

	// Setup modern sidebar navigation
	function setupNavigation() {
		const sidebar = document.getElementById('sidebar');
		if (!sidebar) return;

		// Add modern sidebar class - force it immediately and also after a delay
		sidebar.classList.add('sidebar-modern');
		
		// Also ensure it's applied after other scripts run
		setTimeout(() => {
			sidebar.classList.add('sidebar-modern');
		}, 100);

		// Setup nav links
		const navLinks = document.querySelectorAll('.nav-link[data-nav-target]');
		navLinks.forEach(link => {
			// Add icons if not present
			if (!link.querySelector('.nav-icon')) {
				const icon = document.createElement('span');
				icon.className = 'nav-icon';
				const icons = {
					'dashboard': '📊',
					'profile': '👤',
					'attendance': '📅',
					'leave': '📝',
					'payroll': '💰',
					'benefits': '🎁',
					'performance': '📈',
					'announcements': '📢',
					'chatbot': '🤖',
					'users': '👥',
					'job-postings': '💼',
					'reports': '📊',
					'overtime': '⏰',
					'company-settings': '⚙️'
				};
				const target = link.getAttribute('data-nav-target');
				icon.textContent = icons[target] || '📄';
				link.insertBefore(icon, link.firstChild);
			}

			link.addEventListener('click', (e) => {
				e.preventDefault();
				const target = link.getAttribute('data-nav-target');
				navigateTo(target);
			});
		});

		// Mark active link
		const currentPage = getCurrentPage();
		navLinks.forEach(link => {
			if (link.getAttribute('data-nav-target') === currentPage) {
				link.classList.add('active');
			}
		});
	}

	// Navigate based on role and target
	function navigateTo(target) {
		const routes = {
			admin: {
				'dashboard': '/admin-dashboard-summary.html',
				'users': '/admin-users.html',
				'attendance': '/admin-attendance.html',
				'leave': '/admin-leave.html',
				'overtime': '/admin-overtime.html',
				'payroll': '/admin-payroll.html',
				'benefits': '/admin-benefits.html',
				'performance': '/admin-performance.html',
				'job-postings': '/admin-job-postings.html',
				'announcements': '/admin-announcements.html',
				'reports': '/admin-reports.html',
				'company-settings': '/admin-company-settings.html'
			},
			employee: {
				'dashboard': '/employee-dashboard-summary.html',
				'profile': '/employee-profile.html',
				'attendance': '/employee-attendance.html',
				'leave': '/employee-leave.html',
				'overtime': '/employee-overtime.html',
				'payroll': '/employee-payroll.html',
				'benefits': '/employee-benefits.html',
				'performance': '/employee-performance.html',
				'announcements': '/employee-announcements.html',
				'chatbot': '/employee-chatbot.html',
				'reports': '/employee-reports.html'
			}
		};

		const role = currentUser?.role?.toLowerCase() || 'employee';
		const routeMap = routes[role] || routes.employee;
		const url = routeMap[target] || routeMap.dashboard;

		if (url) {
			window.location.href = url;
		}
	}

	// Get current page identifier
	function getCurrentPage() {
		const path = window.location.pathname;
		if (path.includes('admin-reports') || path.includes('employee-reports') || path.includes('reports.html')) return 'reports';
		if (path.includes('dashboard-summary') || path.includes('dashboard.html')) return 'dashboard';
		if (path.includes('admin-users')) return 'users';
		if (path.includes('admin-attendance')) return 'attendance';
		if (path.includes('admin-leave')) return 'leave';
		if (path.includes('admin-payroll')) return 'payroll';
		if (path.includes('admin-benefits')) return 'benefits';
		if (path.includes('admin-performance')) return 'performance';
		if (path.includes('admin-job-postings')) return 'job-postings';
		if (path.includes('admin-announcements')) return 'announcements';
		if (path.includes('admin-company-settings')) return 'company-settings';
		if (path.includes('admin-overtime')) return 'overtime';
		if (path.includes('employee-profile')) return 'profile';
		if (path.includes('employee-attendance')) return 'attendance';
		if (path.includes('employee-leave')) return 'leave';
		if (path.includes('employee-overtime')) return 'overtime';
		if (path.includes('employee-payroll')) return 'payroll';
		if (path.includes('employee-benefits')) return 'benefits';
		if (path.includes('employee-performance')) return 'performance';
		if (path.includes('employee-announcements')) return 'announcements';
		if (path.includes('employee-chatbot')) return 'chatbot';
		return 'dashboard';
	}

	// Setup search functionality
	function setupSearch() {
		const searchInput = document.getElementById('searchInput');
		if (searchInput) {
			searchInput.addEventListener('input', (e) => {
				const query = e.target.value.toLowerCase();
				// Implement search logic based on page
				performSearch(query);
			});
		}
	}

	// Perform search (to be overridden by page-specific code)
	function performSearch(query) {
		// This will be implemented per page
		console.log('Searching for:', query);
	}

	// Setup logout
	function setupLogout() {
		const logoutBtn = document.getElementById('logoutButton');
		if (logoutBtn) {
			logoutBtn.addEventListener('click', async () => {
				await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
				window.location.href = '/login.html';
			});
		}
	}

	// Update user badge
	function updateUserBadge() {
		const badge = document.getElementById('userBadge');
		if (badge && currentUser) {
			badge.textContent = `${currentUser.username} (${currentUser.role})`;
		}
	}

	// Add search container to page
	function addSearchContainer() {
		const contentArea = document.querySelector('.content-area');
		if (!contentArea) return;

		const existingSearch = contentArea.querySelector('.search-container');
		if (existingSearch) return;

		const searchContainer = document.createElement('div');
		searchContainer.className = 'search-container';
		searchContainer.innerHTML = '<input type="text" id="searchInput" placeholder="Search here">';
		
		const main = contentArea.querySelector('main');
		if (main) {
			contentArea.insertBefore(searchContainer, main);
		} else {
			contentArea.insertBefore(searchContainer, contentArea.firstChild);
		}
	}

	// Add page header
	function addPageHeader(title) {
		const contentArea = document.querySelector('.content-area');
		if (!contentArea) return;

		const existingHeader = contentArea.querySelector('.page-header-modern');
		if (existingHeader) {
			existingHeader.querySelector('.page-title-modern').textContent = title;
			return;
		}

		const header = document.createElement('div');
		header.className = 'page-header-modern';
		header.innerHTML = `<h1 class="page-title-modern">${title}</h1>`;
		
		const main = contentArea.querySelector('main');
		if (main) {
			contentArea.insertBefore(header, main);
		} else {
			contentArea.appendChild(header);
		}
	}

	// Enhance cards
	function enhanceCards() {
		const cards = document.querySelectorAll('.card');
		cards.forEach(card => {
			card.classList.add('card-modern');
		});
	}

	// Enhance buttons
	function enhanceButtons() {
		const buttons = document.querySelectorAll('button:not(.logout-button)');
		buttons.forEach(btn => {
			if (!btn.classList.contains('btn-modern')) {
				btn.classList.add('btn-modern');
			}
		});
	}

	// Enhance inputs
	function enhanceInputs() {
		const inputs = document.querySelectorAll('input:not([type="hidden"]), select, textarea');
		inputs.forEach(input => {
			if (!input.classList.contains('input-modern')) {
				input.classList.add('input-modern');
			}
		});
	}

	// Enhance tables
	function enhanceTables() {
		const tables = document.querySelectorAll('table');
		tables.forEach(table => {
			table.classList.add('table-modern');
		});
	}

	// Apply all enhancements
	function applyEnhancements() {
		enhanceCards();
		enhanceButtons();
		enhanceInputs();
		enhanceTables();
	}

	return {
		init,
		checkAuth,
		getCurrentUser: () => currentUser,
		addSearchContainer,
		addPageHeader,
		applyEnhancements,
		navigateTo
	};
})();

// Auto-initialize when DOM is ready
if (document.readyState === 'loading') {
	document.addEventListener('DOMContentLoaded', () => {
		CommonUI.init().then(() => {
			CommonUI.applyEnhancements();
		});
	});
} else {
	CommonUI.init().then(() => {
		CommonUI.applyEnhancements();
	});
}

