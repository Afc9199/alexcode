const DashboardApp = (() => {
	let session = {
		userId: '',
		username: '',
		role: 'EMPLOYEE'
	};

	const statusBox = document.getElementById('status');

	// Auto-logout after 30 minutes of inactivity
	let inactivityTimer = null;
	const INACTIVITY_TIMEOUT = 30 * 60 * 1000; // 30 minutes in milliseconds (change time , just change the first number)

	const resetInactivityTimer = () => {
		if (inactivityTimer) {
			clearTimeout(inactivityTimer);
		}
		console.log('Inactivity timer reset. Auto-logout in ' + (INACTIVITY_TIMEOUT / 1000) + ' seconds');
		inactivityTimer = setTimeout(() => {
			autoLogout();
		}, INACTIVITY_TIMEOUT);
	};

	const autoLogout = async () => {
		console.log('Auto-logout triggered due to inactivity');
		try {
			await fetch('/api/auth/logout', {
				method: 'POST',
				credentials: 'same-origin'
			});
		} catch (error) {
			console.error('Logout error:', error);
		}
		alert('You have been logged out due to inactivity.');
		window.location.href = '/login.html';
	};

	const initInactivityMonitor = () => {
		console.log('Inactivity monitor initialized. Timeout: ' + (INACTIVITY_TIMEOUT / 1000) + ' seconds');
		
		// Track user activity events
		const activityEvents = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
		
		activityEvents.forEach(event => {
			document.addEventListener(event, resetInactivityTimer, true);
		});

		// Start the timer
		resetInactivityTimer();
	};

	const loadSession = async () => {
		try {
			const response = await fetch('/api/auth/me', {
				credentials: 'same-origin'
			});
			if (response.ok) {
				const data = await response.json();
				session = {
					userId: data.userId || '',
					username: data.username || '',
					role: data.role || 'EMPLOYEE'
				};
				return true;
			}
		} catch (error) {
			console.error('Failed to load session:', error);
		}
		return false;
	};

	const getCurrentPosition = () => {
		return new Promise((resolve, reject) => {
			if (!navigator.geolocation) {
				reject(new Error('This device does not support geolocation.'));
				return;
			}
			navigator.geolocation.getCurrentPosition(resolve, reject, {
				enableHighAccuracy: true,
				timeout: 15000,
				maximumAge: 0
			});
		});
	};

	// Company location configuration (should match backend config)
	const COMPANY_LOCATION = {
		latitude: 3.201388,
		longitude: 101.71495,
		radiusMeters: 500, // Default radius, backend may have different value
		maxAccuracyMeters: 1200
	};

	// Calculate distance between two coordinates using Haversine formula
	const haversineDistanceMeters = (lat1, lon1, lat2, lon2) => {
		const R = 6371000; // Earth radius in meters
		const dLat = (lat2 - lat1) * Math.PI / 180;
		const dLon = (lon2 - lon1) * Math.PI / 180;
		const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
			Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
			Math.sin(dLon / 2) * Math.sin(dLon / 2);
		const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	};

	// Check if location is within allowed radius
	const isLocationValid = (latitude, longitude, accuracyMeters) => {
		if (COMPANY_LOCATION.radiusMeters <= 0) {
			return { valid: true }; // Location check disabled
		}
		const distance = haversineDistanceMeters(
			latitude, longitude,
			COMPANY_LOCATION.latitude, COMPANY_LOCATION.longitude
		);
		if (distance > COMPANY_LOCATION.radiusMeters) {
			return {
				valid: false,
				message: `You are currently outside the company's designated location. Distance from company location: ${Math.round(distance)} meters, allowed range: ${COMPANY_LOCATION.radiusMeters} meters.`,
				distance: Math.round(distance)
			};
		}
		if (accuracyMeters > COMPANY_LOCATION.maxAccuracyMeters) {
			return {
				valid: false,
				message: `Location accuracy is insufficient. Current accuracy: ${Math.round(accuracyMeters)} meters, required accuracy: within ${COMPANY_LOCATION.maxAccuracyMeters} meters.`,
				accuracy: Math.round(accuracyMeters)
			};
		}
		return { valid: true, distance: Math.round(distance) };
	};

	const requireAttendanceLocation = async () => {
		try {
			const position = await getCurrentPosition();
			const locationCheck = isLocationValid(
				position.coords.latitude,
				position.coords.longitude,
				position.coords.accuracy || 0
			);
			if (!locationCheck.valid) {
				throw new Error(locationCheck.message);
			}
			return {
				latitude: position.coords.latitude,
				longitude: position.coords.longitude,
				accuracyMeters: position.coords.accuracy || 0
			};
		} catch (error) {
			if (error.message.includes('outside the company') || error.message.includes('Location accuracy') || error.message.includes('不在公司指定位置') || error.message.includes('定位精度不足')) {
				throw error;
			}
			throw new Error('Location permission is required to clock in/out');
		}
	};

	const isWifiConnection = () => {
		const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
		if (!connection || !connection.type) {
			// If connection API is not available, we can't verify, but backend will check
			return { valid: true, message: null };
		}
		if (connection.type !== 'wifi') {
			return {
				valid: false,
				message: `Current network type is ${connection.type}. Please connect to the company Wi-Fi network before clocking in/out.`,
				networkType: connection.type
			};
		}
		return { valid: true, message: null };
	};

	// Show modal dialog
	const showModal = (options) => {
		const {
			title = 'Notification',
			message,
			type = 'warning', // 'warning' or 'error'
			details = null,
			onConfirm = null,
			confirmText = 'OK'
		} = options;

		// Remove existing modal if any
		const existingModal = document.getElementById('attendanceModal');
		if (existingModal) {
			existingModal.remove();
		}

		const overlay = document.createElement('div');
		overlay.id = 'attendanceModal';
		overlay.className = 'modal-overlay';
		overlay.innerHTML = `
			<div class="modal-dialog">
				<div class="modal-header">
					<h3 class="modal-title">
						<span class="modal-title-icon ${type}">${type === 'error' ? '⚠️' : '⚠️'}</span>
						${title}
					</h3>
					<button class="modal-close" aria-label="Close">×</button>
				</div>
				<div class="modal-body">
					<p class="modal-message">${message}</p>
					${details ? `
						<div class="modal-details">
							<div class="modal-details-title">Details：</div>
							<ul class="modal-details-list">
								${details.map(detail => `<li>${detail}</li>`).join('')}
							</ul>
						</div>
					` : ''}
				</div>
				<div class="modal-footer">
					<button class="modal-button modal-button-primary" id="modalConfirmBtn">${confirmText}</button>
				</div>
			</div>
		`;

		document.body.appendChild(overlay);

		// Show modal with animation
		setTimeout(() => {
			overlay.classList.add('show');
		}, 10);

		// Close handlers
		const closeModal = () => {
			overlay.classList.remove('show');
			setTimeout(() => {
				overlay.remove();
			}, 300);
		};

		overlay.querySelector('.modal-close').addEventListener('click', closeModal);
		overlay.querySelector('#modalConfirmBtn').addEventListener('click', () => {
			if (onConfirm) {
				onConfirm();
			}
			closeModal();
		});

		// Close on overlay click
		overlay.addEventListener('click', (e) => {
			if (e.target === overlay) {
				closeModal();
			}
		});

		// Close on Escape key
		const handleEscape = (e) => {
			if (e.key === 'Escape') {
				closeModal();
				document.removeEventListener('keydown', handleEscape);
			}
		};
		document.addEventListener('keydown', handleEscape);
	};

	const api = async (url, options = {}) => {
		const response = await fetch(url, {
			credentials: 'same-origin',
			...options
		});
		const text = await response.text();
		let payload;
		try {
			payload = text ? JSON.parse(text) : {};
		} catch {
			payload = text;
		}

		if (response.status === 401) {
			showStatus('Session expired. Redirecting to login...');
			window.location.href = '/login.html';
			return { ok: false, status: response.status, payload, rawText: text };
		}
		if (response.status === 403) {
			showStatus('Access denied for this action');
		}
		return { ok: response.ok, status: response.status, payload, rawText: text };
	};

	const showStatus = (message) => {
		if (!statusBox) return;
		statusBox.textContent = message;
		statusBox.style.display = 'block';
		setTimeout(() => (statusBox.style.display = 'none'), 2800);
	};

	const navTargets = {
		employee: {
			profile: '/employee-profile.html',
			attendance: '/employee-attendance.html',
			leave: '/employee-leave.html',
			overtime: '/employee-overtime.html',
			payroll: '/employee-payroll.html',
			benefits: '/employee-benefits.html',
			performance: '/employee-performance.html',
			announcements: '/employee-announcements.html',
			chatbot: '/employee-chatbot.html'
		},
		admin: {
			users: '/admin-users.html',
			attendance: '/admin-attendance.html',
			leave: '/admin-leave.html',
			payroll: '/admin-payroll.html',
			benefits: '/admin-benefits.html',
			performance: '/admin-performance.html',
			'job-postings': '/admin-job-postings.html',
			announcements: '/admin-announcements.html',
			reports: '/admin-reports.html',
			'company-settings': '/admin-company-settings.html'
		}
	};

	const buildNavHref = (path) => {
		return path;
	};

	const initSidebar = ({ scope, active }) => {
		document.querySelectorAll('[data-nav-scope]').forEach(link => {
			const scopeKey = link.dataset.navScope;
			const targetKey = link.dataset.navTarget;
			const path = navTargets[scopeKey]?.[targetKey];
			if (path) {
				link.href = buildNavHref(path);
			}
			if (scopeKey === scope && targetKey === active) {
				link.classList.add('active');
			} else {
				link.classList.remove('active');
			}
		});
	};

	const renderUserBadge = () => {
		const badge = document.getElementById('userBadge');
		if (!badge) return;
		const parts = [];
		if (session.username) parts.push(session.username);
		if (session.role) parts.push(session.role);
		badge.textContent = parts.join(' · ') || 'Signed in';
	};

	const attachLogout = () => {
		const button = document.getElementById('logoutButton');
		if (!button) {
			return;
		}
		button.addEventListener('click', async () => {
			const res = await fetch('/api/auth/logout', {
				method: 'POST',
				credentials: 'same-origin'
			});
			if (res.ok) {
				showStatus('Logged out');
				setTimeout(() => (window.location.href = '/login.html'), 500);
			} else {
				showStatus('Logout failed');
			}
		});
	};

	const prefillForms = (formIds) => {
		formIds.forEach(id => {
			const form = document.getElementById(id);
			if (!form) return;
			if (form.userId) {
				form.userId.value = session.userId;
			}
			if (form.decidedBy) {
				form.decidedBy.value = session.userId;
			}
		});
	};

	const initEmployeeProfile = () => {
		const form = document.getElementById('employeeProfileForm');
		const employeeDepartmentSelect = document.getElementById('employeeDepartmentSelect');
		
		// Load departments for employee profile
		const loadDepartmentsForEmployee = async () => {
			if (!employeeDepartmentSelect) return;
			const res = await api('/api/employee/departments');
			if (res.ok && res.payload) {
				employeeDepartmentSelect.innerHTML = '<option value="">-- Select Department --</option>';
				res.payload.forEach(dept => {
					const option = document.createElement('option');
					option.value = dept.name;
					option.textContent = dept.name;
					employeeDepartmentSelect.appendChild(option);
				});
			}
		};
		
		// Load departments on initialization
		loadDepartmentsForEmployee();
		
		if (!form) return;

		// Auto-populate userId from session and load profile
		if (session.userId && form.userId) {
			form.userId.value = session.userId;
			// Auto-load profile on page load
			setTimeout(async () => {
				const res = await api(`/api/employee/profile/${session.userId}`);
				if (res.ok) {
					form.employeeId.value = res.payload.employeeId || '';
					form.username.value = res.payload.username || '';
					form.fullName.value = res.payload.fullName || '';
					form.gender.value = res.payload.gender || '';
					form.age.value = res.payload.age || '';
					form.race.value = res.payload.race || '';
					form.religion.value = res.payload.religion || '';
					form.address.value = res.payload.address || '';
					form.maritalStatus.value = res.payload.maritalStatus || '';
					form.bankName.value = 'Malayan Banking Berhad';
					form.bankAccountNumber.value = res.payload.bankAccountNumber || '';
					form.epfNumber.value = res.payload.epfNumber || '';
					form.icNumber.value = res.payload.icNumber || '';
					// Set old password field with IC number (default password)
					const oldPasswordInput = document.getElementById('oldPassword');
					if (oldPasswordInput && res.payload.icNumber) {
						oldPasswordInput.value = res.payload.icNumber;
					}
					form.taxNumber.value = res.payload.taxNumber || '';
					form.numberOfChildren.value = res.payload.numberOfChildren !== undefined ? res.payload.numberOfChildren : 0;
					form.nationality.value = res.payload.nationality || '';
					form.residentStatus.value = res.payload.residentStatus || '';
					form.spouseWorking.value = res.payload.spouseWorking || '';
					form.email.value = res.payload.email || '';
					// Set department dropdown value
					if (employeeDepartmentSelect) {
						employeeDepartmentSelect.value = res.payload.department || '';
						// Also set hidden input for form submission (since select is disabled)
						const departmentHidden = document.getElementById('employeeDepartmentHidden');
						if (departmentHidden) {
							departmentHidden.value = res.payload.department || '';
						}
					} else {
						form.department.value = res.payload.department || '';
					}
					form.jobTitle.value = res.payload.jobTitle || '';
					form.basicSalary.value = res.payload.basicSalary != null ? 'RM ' + res.payload.basicSalary.toFixed(2) : 'N/A';
					
					// Remove +60 prefix for display
					if (res.payload.contactNumber && res.payload.contactNumber.startsWith('+60')) {
						form.contactNumber.value = res.payload.contactNumber.substring(3);
					} else {
						form.contactNumber.value = res.payload.contactNumber || '';
					}
					
					// Set date of birth
					if (res.payload.dateOfBirth) {
						form.dateOfBirth.value = res.payload.dateOfBirth;
					}
					
					// Set SOCSO number
					if (res.payload.socsoNumber) {
						form.socsoNumber.value = res.payload.socsoNumber;
					}
					
					// Set date of hire
					if (res.payload.dateOfHire) {
						form.dateOfHire.value = res.payload.dateOfHire;
					}
					
					// Set emergency contact information
					if (res.payload.emergencyContactName) {
						form.emergencyContactName.value = res.payload.emergencyContactName;
					}
					if (res.payload.emergencyContactRelationship) {
						form.emergencyContactRelationship.value = res.payload.emergencyContactRelationship;
					}
					// Remove +60 prefix for emergency contact number display
					if (res.payload.emergencyContactNumber && res.payload.emergencyContactNumber.startsWith('+60')) {
						form.emergencyContactNumber.value = res.payload.emergencyContactNumber.substring(3);
					} else if (res.payload.emergencyContactNumber) {
						form.emergencyContactNumber.value = res.payload.emergencyContactNumber;
					}
					
					// Handle passport number visibility based on nationality
					if (res.payload.nationality === 'Non-Malaysian') {
						const passportGroup = document.getElementById('profilePassportNumberGroup');
						if (passportGroup) {
							passportGroup.style.display = 'block';
						}
						if (res.payload.passportNumber) {
							form.passportNumber.value = res.payload.passportNumber;
						}
					}
				}
			}, 100);
		}

		// Handle Load Profile button
		const loadProfileButton = document.querySelector('button[data-action="fetchProfile"]');
		if (loadProfileButton) {
			loadProfileButton.addEventListener('click', async () => {
				const userId = form.userId.value || session.userId;
				if (!userId) {
					showStatus('User ID required');
					return;
				}
				const res = await api(`/api/employee/profile/${userId}`);
				if (res.ok) {
					// Reload profile data (same logic as auto-load)
					form.employeeId.value = res.payload.employeeId || '';
					form.username.value = res.payload.username || '';
					form.fullName.value = res.payload.fullName || '';
					form.gender.value = res.payload.gender || '';
					form.age.value = res.payload.age || '';
					form.race.value = res.payload.race || '';
					form.religion.value = res.payload.religion || '';
					form.address.value = res.payload.address || '';
					form.maritalStatus.value = res.payload.maritalStatus || '';
					form.bankName.value = 'Malayan Banking Berhad';
					form.bankAccountNumber.value = res.payload.bankAccountNumber || '';
					form.epfNumber.value = res.payload.epfNumber || '';
					form.icNumber.value = res.payload.icNumber || '';
					const oldPasswordInput = document.getElementById('oldPassword');
					if (oldPasswordInput && res.payload.icNumber) {
						oldPasswordInput.value = res.payload.icNumber;
					}
					form.taxNumber.value = res.payload.taxNumber || '';
					form.numberOfChildren.value = res.payload.numberOfChildren !== undefined ? res.payload.numberOfChildren : 0;
					form.nationality.value = res.payload.nationality || '';
					form.residentStatus.value = res.payload.residentStatus || '';
					form.spouseWorking.value = res.payload.spouseWorking || '';
					form.email.value = res.payload.email || '';
					if (employeeDepartmentSelect) {
						employeeDepartmentSelect.value = res.payload.department || '';
						const departmentHidden = document.getElementById('employeeDepartmentHidden');
						if (departmentHidden) {
							departmentHidden.value = res.payload.department || '';
						}
					} else {
						form.department.value = res.payload.department || '';
					}
					form.jobTitle.value = res.payload.jobTitle || '';
					form.basicSalary.value = res.payload.basicSalary != null ? 'RM ' + res.payload.basicSalary.toFixed(2) : 'N/A';
					if (res.payload.contactNumber && res.payload.contactNumber.startsWith('+60')) {
						form.contactNumber.value = res.payload.contactNumber.substring(3);
					} else {
						form.contactNumber.value = res.payload.contactNumber || '';
					}
					if (res.payload.dateOfBirth) {
						form.dateOfBirth.value = res.payload.dateOfBirth;
					}
					if (res.payload.socsoNumber) {
						form.socsoNumber.value = res.payload.socsoNumber;
					}
					if (res.payload.dateOfHire) {
						form.dateOfHire.value = res.payload.dateOfHire;
					}
					if (res.payload.emergencyContactName) {
						form.emergencyContactName.value = res.payload.emergencyContactName;
					}
					if (res.payload.emergencyContactRelationship) {
						form.emergencyContactRelationship.value = res.payload.emergencyContactRelationship;
					}
					if (res.payload.emergencyContactNumber && res.payload.emergencyContactNumber.startsWith('+60')) {
						form.emergencyContactNumber.value = res.payload.emergencyContactNumber.substring(3);
					} else if (res.payload.emergencyContactNumber) {
						form.emergencyContactNumber.value = res.payload.emergencyContactNumber;
					}
					if (res.payload.nationality === 'Non-Malaysian') {
						const passportGroup = document.getElementById('profilePassportNumberGroup');
						if (passportGroup) {
							passportGroup.style.display = 'block';
						}
						if (res.payload.passportNumber) {
							form.passportNumber.value = res.payload.passportNumber;
						}
					}
					showStatus('Profile loaded successfully');
				} else {
					showStatus('Failed to load profile');
				}
			});
		}

		// Handle password change button
		const changePasswordButton = document.getElementById('changePasswordButton');
		if (changePasswordButton) {
			changePasswordButton.addEventListener('click', async () => {
				const oldPassword = document.getElementById('oldPassword').value.trim();
				const newPassword = document.getElementById('newPassword').value.trim();
				const confirmNewPassword = document.getElementById('confirmNewPassword').value.trim();
				
				// Validate old password is provided
				if (!oldPassword) {
					showStatus('Old password is required');
					document.getElementById('oldPassword').focus();
					return;
				}
				
				// Validate new password is provided
				if (!newPassword) {
					showStatus('New password is required');
					document.getElementById('newPassword').focus();
					return;
				}
				
				// Validate confirm password is provided
				if (!confirmNewPassword) {
					showStatus('Confirm password is required');
					document.getElementById('confirmNewPassword').focus();
					return;
				}
				
				// Helper function to validate password format (8-15 chars, letter, digit, special char)
				const validatePasswordFormat = (password, fieldName) => {
					if (password.length < 8) {
						return { valid: false, message: `${fieldName} must be at least 8 characters long` };
					}
					if (password.length > 15) {
						return { valid: false, message: `${fieldName} must not exceed 15 characters` };
					}
					if (!/[a-zA-Z]/.test(password)) {
						return { valid: false, message: `${fieldName} must include at least one letter` };
					}
					if (!/[0-9]/.test(password)) {
						return { valid: false, message: `${fieldName} must include at least one digit` };
					}
					// Special characters: !@#$%^&*()_+-=[]{};':"\\|,.<>/?
					if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(password)) {
						return { valid: false, message: `${fieldName} must include at least one special character` };
					}
					return { valid: true };
				};
				
				// Validate new password format
				const newPasswordValidation = validatePasswordFormat(newPassword, 'New password');
				if (!newPasswordValidation.valid) {
					showStatus(newPasswordValidation.message);
					document.getElementById('newPassword').focus();
					return;
				}
				
				// Validate confirm new password format
				const confirmPasswordValidation = validatePasswordFormat(confirmNewPassword, 'Confirm new password');
				if (!confirmPasswordValidation.valid) {
					showStatus(confirmPasswordValidation.message);
					document.getElementById('confirmNewPassword').focus();
					return;
				}
				
				// Validate new password and confirm password match (must be exactly the same)
				if (newPassword !== confirmNewPassword) {
					showStatus('New password and confirm password must match exactly');
					document.getElementById('confirmNewPassword').focus();
					return;
				}
				
				// Call password change API
				const userId = form.userId.value || session.userId;
				if (!userId) {
					showStatus('User ID required');
					return;
				}
				
				try {
					const res = await api(`/api/employee/profile/${userId}/change-password`, {
						method: 'POST',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify({
							oldPassword: oldPassword,
							newPassword: newPassword,
							confirmPassword: confirmNewPassword
						})
					});
					
					if (res.ok) {
						showStatus('Password changed successfully');
						// Clear password fields
						document.getElementById('oldPassword').value = '';
						document.getElementById('newPassword').value = '';
						document.getElementById('confirmNewPassword').value = '';
					} else {
						showStatus(res.message || res.error || 'Failed to change password');
					}
				} catch (error) {
					showStatus('Failed to change password: ' + error.message);
				}
			});
		}

		// Disable form submission - employees can only change password
		form.addEventListener('submit', async (event) => {
			event.preventDefault();
			showStatus('Profile updates are not allowed. Please contact HR to update your profile information.');
			return;
			const formData = new FormData(form);
			const userId = formData.get('userId') || session.userId;
			
			// Clean contact number: remove spaces and dashes, add +60
			let contactNumber = formData.get('contactNumber') || '';
			contactNumber = contactNumber.replace(/[\s-]/g, '');
			if (contactNumber && !contactNumber.startsWith('+60')) {
				contactNumber = '+60' + contactNumber;
			}

			// Validate contact number (optional, 9-10 digits after +60 if provided)
			if (contactNumber && contactNumber !== '+60') {
				const digits = contactNumber.substring(3);
				if (!/^\d{9,10}$/.test(digits)) {
					showStatus('Contact number must be 9-10 digits');
					return;
				}
			}

			// Validate email (optional, format validation if provided)
			const email = formData.get('email');
			if (email && email.trim()) {
				if (email.includes(' ')) {
					showStatus('Email cannot contain spaces');
					return;
				}
				if (!email.includes('@')) {
					showStatus('Email must contain @ symbol');
					return;
				}
				const parts = email.split('@');
				if (parts.length !== 2 || !parts[1].includes('.')) {
					showStatus('Email must contain a dot (.) after @');
					return;
				}
			}

			// Validate full name (optional, max 50 characters)
			const fullName = formData.get('fullName');
			if (fullName && fullName.length > 50) {
				showStatus('Full name must not exceed 50 characters');
				return;
			}

			// Validate department (max 50 characters)
			const department = formData.get('department');
			if (department && department.length > 50) {
				showStatus('Department must not exceed 50 characters');
				return;
			}

			// Validate job title (max 50 characters)
			const jobTitle = formData.get('jobTitle');
			if (jobTitle && jobTitle.length > 50) {
				showStatus('Job title must not exceed 50 characters');
				return;
			}

			// Validate age (optional, min 18 if provided)
			const age = formData.get('age');
			if (age && age.trim() !== '') {
				const ageNum = parseInt(age, 10);
				if (isNaN(ageNum) || ageNum < 18) {
					showStatus('Age must be 18 or above');
					return;
				}
			}

			// Validate race (letters only, max 30) - optional for employee
			const race = formData.get('race');
			if (race && race.trim()) {
				if (!/^[a-zA-Z\s]+$/.test(race.trim())) {
					showStatus('Race must contain letters only');
					return;
				}
				if (race.trim().length > 30) {
					showStatus('Race must not exceed 30 characters');
					return;
				}
			}

			// Validate religion (letters only, max 30) - optional for employee
			const religion = formData.get('religion');
			if (religion && religion.trim()) {
				if (!/^[a-zA-Z\s]+$/.test(religion.trim())) {
					showStatus('Religion must contain letters only');
					return;
				}
				if (religion.trim().length > 30) {
					showStatus('Religion must not exceed 30 characters');
					return;
				}
			}

			// Validate address (max 250 characters) - optional for employee
			const address = formData.get('address');
			if (address && address.trim().length > 250) {
				showStatus('Address must not exceed 250 characters');
				return;
			}

			// Bank name is fixed to "Malayan Banking Berhad", no validation needed

			// Remove spaces and dashes from Bank Account Number and validate
			let bankAccountNumber = formData.get('bankAccountNumber') || '';
			if (bankAccountNumber && bankAccountNumber.trim()) {
				bankAccountNumber = bankAccountNumber.replace(/[\s\-]/g, '');
				if (!/^\d{10,16}$/.test(bankAccountNumber)) {
					showStatus('Bank account number must be 10-16 digits only');
					return;
				}
			}

			// Validate EPF number (exactly 8 digits) - optional for employee
			const epfNumber = formData.get('epfNumber');
			if (epfNumber && epfNumber.trim()) {
				if (!/^\d{8}$/.test(epfNumber.trim())) {
					showStatus('EPF number must be exactly 8 digits');
					return;
				}
			}

			// Validate IC number (optional, 12 digits if provided, no spaces, dashes will be removed)
			const icNumber = formData.get('icNumber') || '';
			if (icNumber && icNumber.trim()) {
				// Check for spaces explicitly
				if (icNumber.includes(' ')) {
					showStatus('IC number cannot contain spaces');
					return;
				}
				// Remove all non-digit characters for validation
				const cleanedIc = icNumber.replace(/[^\d]/g, '');
				if (cleanedIc.length !== 12) {
					showStatus('IC number must be exactly 12 digits');
					return;
				}
				if (!/^\d{12}$/.test(cleanedIc)) {
					showStatus('IC number must contain only digits');
					return;
				}
			}

			// Validate passport number (required when Non-Malaysian, no format validation)
			const nationality = formData.get('nationality') || '';
			let passportNumber = '';
			if (nationality === 'Non-Malaysian') {
				passportNumber = formData.get('passportNumber') || '';
				if (!passportNumber || !passportNumber.trim()) {
					showStatus('Passport number is required for Non-Malaysian');
					return;
				}
			}

			// Validate SOCSO number (optional, 12 digits if provided, no spaces, dashes will be removed - same as IC number)
			const socsoNumber = formData.get('socsoNumber') || '';
			if (socsoNumber && socsoNumber.trim()) {
				// Check for spaces explicitly
				if (socsoNumber.includes(' ')) {
					showStatus('SOCSO number cannot contain spaces');
					return;
				}
				// Remove all non-digit characters for validation
				const cleanedSocso = socsoNumber.replace(/[^\d]/g, '');
				if (cleanedSocso.length !== 12) {
					showStatus('SOCSO number must be exactly 12 digits');
					return;
				}
				if (!/^\d{12}$/.test(cleanedSocso)) {
					showStatus('SOCSO number must contain only digits');
					return;
				}
			}

			// Validate tax number (optional, must start with "IG" prefix if provided, then 9-11 digits)
			const taxNumber = formData.get('taxNumber');
			if (taxNumber && taxNumber.trim()) {
				if (!/^IG\d{9,11}$/i.test(taxNumber.trim())) {
					showStatus('Tax number must start with "IG" prefix, followed by 9 to 11 digits');
					return;
				}
			}

			// Validate emergency contact number (optional, same validation as contact number if provided)
			let emergencyContactNumber = formData.get('emergencyContactNumber') || '';
			if (emergencyContactNumber && emergencyContactNumber.trim()) {
				emergencyContactNumber = emergencyContactNumber.replace(/[\s-]/g, '');
				if (!emergencyContactNumber.startsWith('+60')) {
					emergencyContactNumber = '+60' + emergencyContactNumber;
				}
				if (emergencyContactNumber && emergencyContactNumber !== '+60') {
					const emergencyDigits = emergencyContactNumber.substring(3);
					if (!/^\d{9,10}$/.test(emergencyDigits)) {
						showStatus('Emergency contact number must be 9-10 digits');
						return;
					}
				}
			}

			// Validate emergency contact name (optional, max 100 characters if provided)
			const emergencyContactName = formData.get('emergencyContactName');
			if (emergencyContactName && emergencyContactName.trim()) {
				if (emergencyContactName.trim().length > 100) {
					showStatus('Emergency contact name must not exceed 100 characters');
					return;
				}
			}

			// Relationship is optional (no validation needed)

			// Number of children is optional (no validation needed)

			// Spouse working is optional (no validation needed)

			// Nationality is optional (no validation needed, but passport number required if Non-Malaysian)
			if (nationality === 'Non-Malaysian') {
				const passportNumber = formData.get('passportNumber') || '';
				if (!passportNumber || !passportNumber.trim()) {
					showStatus('Passport number is required for Non-Malaysian');
					return;
				}
			}

			// Resident status is optional (no validation needed)

			// Date of birth is optional (no validation needed)

			// Date of hire is optional (no validation needed)

			// Department is optional (no validation needed)

			// Job title is optional (no validation needed)

		// Clean IC number (remove dashes and spaces, store only 12 digits) if provided
		const cleanedIcForStorage = icNumber && icNumber.trim() ? icNumber.replace(/[^\d]/g, '') : '';
		
		// Clean SOCSO number (remove dashes and spaces, store only 12 digits) if provided
		const cleanedSocsoForStorage = socsoNumber && socsoNumber.trim() ? socsoNumber.replace(/[^\d]/g, '') : '';

		const payload = {
			fullName: fullName ? fullName.trim() : '',
			email: email ? email.trim() : '',
			department: department ? department.trim() : '',
			jobTitle: jobTitle ? jobTitle.trim() : '',
			contactNumber: contactNumber || '',
			gender: formData.get('gender') || '',
			age: age && age.trim() ? parseInt(age, 10) : null,
			race: race ? race.trim() : '',
			religion: religion ? religion.trim() : '',
			address: address ? address.trim() : '',
			maritalStatus: formData.get('maritalStatus') || '',
			bankName: 'Malayan Banking Berhad',
			bankAccountNumber: bankAccountNumber || '',
			epfNumber: epfNumber ? epfNumber.trim() : '',
			icNumber: cleanedIcForStorage || '',
			passportNumber: passportNumber ? passportNumber.trim() : '',
			taxNumber: taxNumber ? taxNumber.trim() : '',
			numberOfChildren: formData.get('numberOfChildren') !== null && formData.get('numberOfChildren') !== undefined && formData.get('numberOfChildren') !== '' ? parseInt(formData.get('numberOfChildren'), 10) : null,
			nationality: nationality ? nationality.trim() : '',
			residentStatus: formData.get('residentStatus') ? formData.get('residentStatus').trim() : '',
			spouseWorking: formData.get('spouseWorking') ? formData.get('spouseWorking').trim() : '',
			socsoNumber: cleanedSocsoForStorage || '',
			dateOfBirth: formData.get('dateOfBirth') || null,
			dateOfHire: formData.get('dateOfHire') || null,
			emergencyContactName: emergencyContactName ? emergencyContactName.trim() : '',
			emergencyContactRelationship: formData.get('emergencyContactRelationship') ? formData.get('emergencyContactRelationship').trim() : '',
			emergencyContactNumber: emergencyContactNumber || ''
		};

			const res = await api(`/api/employee/profile/${userId}`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload)
			});
			if (res.ok) {
				showStatus('Profile updated successfully');
			} else {
				// Extract error message from Spring Boot error response
				// Spring Boot ResponseStatusException can return errors in different formats:
				// Format 1: { "timestamp": "...", "status": 409, "error": "Conflict", "message": "IC number already exists", "path": "..." }
				// Format 2: Just the message string
				// Format 3: { "message": "IC number already exists" }
				let errorMessage = 'Update failed';
				if (typeof res.payload === 'string') {
					errorMessage = res.payload;
				} else if (res.payload && typeof res.payload === 'object') {
					// Try multiple possible fields where the error message might be
					// Priority: message > detail > error (but skip "error" if it's just "Conflict", "Bad Request", etc.)
					errorMessage = res.payload.message || 
								   res.payload.detail ||
								   (res.payload.error && !['Conflict', 'Bad Request', 'Not Found', 'Unauthorized', 'Forbidden'].includes(res.payload.error) ? res.payload.error : null) ||
								   (res.payload.errors && Array.isArray(res.payload.errors) && res.payload.errors[0]?.defaultMessage) ||
								   'Update failed';
				}
				// If we still have a generic error, try to extract from raw text
				if (errorMessage === 'Update failed' && res.rawText) {
					try {
						const parsed = JSON.parse(res.rawText);
						errorMessage = parsed.message || parsed.detail || errorMessage;
					} catch {
						// If raw text is not JSON, use it as the error message
						if (res.rawText && res.rawText.trim() && res.rawText.trim() !== 'Conflict') {
							errorMessage = res.rawText.trim();
						}
					}
				}
				showStatus(errorMessage);
			}
		});
	};

	const initEmployeeBenefits = () => {
		const benefitsResultBox = document.getElementById('employeeBenefitsResult');

		const renderEmployeeBenefits = (benefits) => {
			if (!benefitsResultBox) return;

			if (!benefits || benefits.length === 0) {
				benefitsResultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 2rem;">No benefits assigned.</p>';
				return;
			}

			const table = `
				<table style="width: 100%; border-collapse: collapse;">
					<thead>
						<tr style="background-color: #f5f5f5;">
							<th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #ddd;">Benefit ID</th>
							<th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #ddd;">Benefit Name</th>
							<th style="padding: 0.75rem; text-align: right; border-bottom: 2px solid #ddd;">Benefit Amount (RM)</th>
							<th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #ddd;">Assigned Date</th>
						</tr>
					</thead>
					<tbody>
						${benefits.map(benefit => {
							const amount = benefit.benefitAmount != null ? parseFloat(benefit.benefitAmount).toFixed(2) : 'N/A';
							const assignedDate = benefit.assignedAt ? new Date(benefit.assignedAt).toLocaleDateString() : 'N/A';
							return `
							<tr style="border-bottom: 1px solid #eee;">
								<td style="padding: 0.75rem; font-weight: 500; color: #007bff;">${benefit.benefitId || 'N/A'}</td>
								<td style="padding: 0.75rem;">${benefit.benefitName || 'N/A'}</td>
								<td style="padding: 0.75rem; text-align: right; font-weight: 500;">${amount}</td>
								<td style="padding: 0.75rem;">${assignedDate}</td>
							</tr>
						`;}).join('')}
					</tbody>
				</table>
			`;
			benefitsResultBox.innerHTML = table;
		};

		const loadEmployeeBenefits = async () => {
			if (!benefitsResultBox) return;
			benefitsResultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 2rem;">Loading benefits...</p>';
			const res = await api(`/api/employee/benefits/${session.userId}`);
			if (res.ok && res.payload) {
				renderEmployeeBenefits(res.payload);
			} else {
				benefitsResultBox.innerHTML = '<p style="text-align: center; color: #888;">Failed to load benefits.</p>';
			}
		};

		loadEmployeeBenefits();
	};

		const initEmployeeKPIs = () => {
		const resultBox = document.getElementById('employeeKpiResult');
		const searchInput = document.getElementById('employeeKpiSearchInput');
		const searchButton = document.getElementById('employeeKpiSearchButton');
		const progressForm = document.getElementById('kpiProgressForm');
		const progressAssignmentIdInput = document.getElementById('kpiProgressAssignmentId');
		const progressNameInput = document.getElementById('kpiProgressName');
		const progressTargetText = document.getElementById('kpiProgressTarget');
		const progressCurrentText = document.getElementById('kpiProgressCurrent');
		const progressEvidenceInfo = document.getElementById('kpiProgressEvidenceInfo');
		const progressStatusBadge = document.getElementById('kpiProgressStatusBadge');
		const progressLockNotice = document.getElementById('kpiProgressLockNotice');
		const progressValueInput = document.getElementById('kpiProgressValue');
		const progressPreviewText = document.getElementById('kpiProgressPreview');
		const progressEvidenceInput = document.getElementById('kpiProgressEvidence');
		const progressCancelButton = document.getElementById('kpiProgressCancelButton');

		let selectedAssignmentId = null;
		let currentTargetValue = 0;
		let currentAccumulatedValue = 0;
		const kpiAssignmentCache = new Map();
		const sanitizeText = (text) => {
			const div = document.createElement('div');
			div.textContent = text ?? '';
			return div.innerHTML;
		};
		const getStatusBadgeMarkup = (status) => {
			const normalized = (status || 'PENDING').toUpperCase();
			const styles = {
				COMPLETED: { className: 'status-pill-modern success', label: 'Completed' },
				INCOMPLETE: { className: 'status-pill-modern danger', label: 'Incomplete' },
				PENDING: { className: 'status-pill-modern secondary', label: 'Pending' }
			};
			const theme = styles[normalized] || styles.PENDING;
			return `<span class="${theme.className}">${theme.label}</span>`;
		};
		const isAssignmentLocked = (assignment) => {
			if (!assignment || !assignment.dueDate) {
				return false;
			}
			const today = new Date();
			today.setHours(0, 0, 0, 0);
			const dueDate = new Date(assignment.dueDate);
			dueDate.setHours(0, 0, 0, 0);
			return today.getTime() >= dueDate.getTime();
		};

		// Load KPIs automatically on page load
		// Use a small delay to ensure session is initialized
		setTimeout(() => {
			if (resultBox) {
				if (session.userId) {
					loadEmployeeKpis();
				} else {
					console.warn('Session userId not available, retrying in 500ms...');
					setTimeout(() => {
						if (session.userId && resultBox) {
							loadEmployeeKpis();
						} else {
							resultBox.innerHTML = '<p style="text-align: center; color: #d32f2f; padding: 2rem;">Unable to load KPIs. Please refresh the page.</p>';
						}
					}, 500);
				}
			}
		}, 100);

		const resetProgressForm = () => {
			selectedAssignmentId = null;
			currentTargetValue = 0;
			currentAccumulatedValue = 0;
			if (progressAssignmentIdInput) progressAssignmentIdInput.value = '';
			if (progressNameInput) progressNameInput.value = '';
			if (progressTargetText) progressTargetText.textContent = 'N/A';
			if (progressCurrentText) progressCurrentText.textContent = 'N/A';
			if (progressEvidenceInfo) progressEvidenceInfo.textContent = 'No file uploaded';
			if (progressValueInput) progressValueInput.value = '';
			if (progressEvidenceInput) progressEvidenceInput.value = '';
			if (progressPreviewText) progressPreviewText.textContent = 'Progress Preview: 0%';
			if (progressLockNotice) progressLockNotice.style.display = 'none';
			if (progressStatusBadge) progressStatusBadge.innerHTML = getStatusBadgeMarkup();
			if (progressValueInput) progressValueInput.disabled = false;
			if (progressEvidenceInput) progressEvidenceInput.disabled = false;
			const submitButton = document.getElementById('kpiProgressSubmitButton');
			if (submitButton) submitButton.disabled = false;
		};

		const updateProgressPreview = () => {
			if (!progressPreviewText) return;
			const rawValue = progressValueInput?.value?.trim() || '';
			if (!currentTargetValue || !/^\d+$/.test(rawValue)) {
				progressPreviewText.textContent = 'Progress Preview: 0%';
				return;
			}
			const value = parseFloat(rawValue);
			const pct = Math.min(100, ((currentAccumulatedValue + value) / currentTargetValue) * 100);
			progressPreviewText.textContent = `Progress Preview: ${pct.toFixed(2)}%`;
		};

		if (progressValueInput) {
			progressValueInput.addEventListener('input', () => {
				const numericValue = progressValueInput.value.replace(/[^\d]/g, '');
				progressValueInput.value = numericValue;
				updateProgressPreview();
			});
		}

		if (progressCancelButton) {
			progressCancelButton.addEventListener('click', (event) => {
				event.preventDefault();
				resetProgressForm();
			});
		}

		const formatDate = (value) => {
			if (!value) return 'N/A';
			try {
				if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
					const [year, month, day] = value.split('-').map(Number);
					return new Date(year, month - 1, day).toLocaleDateString();
				}
				return new Date(value).toLocaleDateString();
			} catch {
				return value;
			}
		};

		const renderEmployeeKpiTable = (assignments) => {
			if (!resultBox) return;
			if (!assignments || assignments.length === 0) {
				resultBox.innerHTML = '<div class="empty-state-modern">No KPIs assigned yet.</div>';
				return;
			}

			kpiAssignmentCache.clear();

			const table = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>KPI ID</th>
								<th>KPI Name</th>
								<th>Description</th>
								<th>Target</th>
								<th>Current Progress</th>
								<th>Progress %</th>
								<th>Evidence</th>
								<th>Due Date</th>
								<th style="text-align: right;">Bonus (RM)</th>
								<th>Status</th>
								<th style="text-align: center;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${assignments.map(item => {
								kpiAssignmentCache.set(item.id, item);
								const bonusAmount = item.bonusAmount != null ? parseFloat(item.bonusAmount).toFixed(2) : 'N/A';
								const currentValue = item.currentProgressValue != null ? item.currentProgressValue : 0;
								const progressPercent = item.progressPercentage != null ? item.progressPercentage.toFixed(2) + '%' : '0%';
								const evidenceLink = item.evidenceFilename
									? `<a href="/api/employee/kpis/evidence/${encodeURIComponent(item.evidenceFilename)}" target="_blank" class="action-link-modern">View</a>`
									: '<span style="color: #999;">No evidence</span>';
								const statusBadge = getStatusBadgeMarkup(item.status);
								return `
									<tr>
										<td class="text-primary font-medium">${item.kpiId || 'N/A'}</td>
										<td class="font-medium">${item.kpiName || 'N/A'}</td>
										<td class="text-muted">${item.description || 'No description'}</td>
										<td>${item.measurableValue || 'N/A'}</td>
										<td>${currentValue} / ${item.measurableValue || '0'}</td>
										<td>${progressPercent}</td>
										<td>${evidenceLink}</td>
										<td>${formatDate(item.dueDate)}</td>
										<td class="text-right font-medium">RM ${bonusAmount}</td>
										<td>${statusBadge}</td>
										<td style="text-align: center;">
											<button class="action-btn-modern" onclick="window.DashboardApp.openKpiProgressForm('${item.id}')">
												Update Progress
											</button>
										</td>
									</tr>
								`;
							}).join('')}
						</tbody>
					</table>
				</div>
			`;
			resultBox.innerHTML = table;
		};

		const loadEmployeeKpis = async (searchTerm = '') => {
			if (!resultBox) {
				console.error('KPI result box not found');
				return;
			}
			if (!session.userId) {
				console.error('Session userId not set');
				resultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 2rem;">User session not initialized. Please refresh the page.</p>';
				return;
			}
			resultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 2rem;">Loading your KPIs...</p>';
			try {
				const res = await api(`/api/employee/kpis/${session.userId}`);
				console.log('KPI API response:', res); // Debug log
				if (res.ok && Array.isArray(res.payload)) {
					let assignments = res.payload;
					if (searchTerm) {
						const term = searchTerm.toLowerCase();
						assignments = assignments.filter(item =>
							(item.kpiId && item.kpiId.toLowerCase().includes(term)) ||
							(item.kpiName && item.kpiName.toLowerCase().includes(term)) ||
							(item.description && item.description.toLowerCase().includes(term)) ||
							(item.measurableValue && item.measurableValue.toLowerCase().includes(term))
						);
					}
					renderEmployeeKpiTable(assignments);
					if (selectedAssignmentId) {
						const latest = kpiAssignmentCache.get(selectedAssignmentId);
						if (latest) {
							window.DashboardApp.openKpiProgressForm(selectedAssignmentId, false);
						} else {
							resetProgressForm();
						}
					}
				} else {
					const errorMsg = res.message || res.payload?.message || res.error || 'Unknown error';
					console.error('Failed to load KPIs:', res);
					resultBox.innerHTML = `<p style="text-align: center; color: #d32f2f; padding: 2rem;">Failed to load KPIs: ${errorMsg}</p>`;
				}
			} catch (error) {
				console.error('Error loading KPIs:', error);
				resultBox.innerHTML = `<p style="text-align: center; color: #d32f2f; padding: 2rem;">Error loading KPIs: ${error.message || 'Unknown error'}</p>`;
			}
		};

		if (searchButton) {
			searchButton.addEventListener('click', () => {
				loadEmployeeKpis(searchInput?.value.trim() || '');
			});
		}

		if (searchInput) {
			searchInput.addEventListener('keypress', (event) => {
				if (event.key === 'Enter') {
					event.preventDefault();
					loadEmployeeKpis(searchInput.value.trim());
				}
			});
		}


		window.DashboardApp = window.DashboardApp || {};
		window.DashboardApp.openKpiProgressForm = (assignmentId, scrollIntoView = true) => {
			const assignment = kpiAssignmentCache.get(assignmentId);
			if (!assignment || !progressForm || !progressNameInput || !progressTargetText || !progressCurrentText || !progressEvidenceInfo || !progressValueInput || !progressEvidenceInput) {
				return;
			}
			selectedAssignmentId = assignmentId;
			currentTargetValue = parseFloat(assignment.measurableValue || '0') || 0;
			progressAssignmentIdInput.value = assignmentId;
			progressNameInput.value = assignment.kpiName || assignment.kpiId || 'KPI';
			progressTargetText.textContent = assignment.measurableValue || 'N/A';
			currentAccumulatedValue = assignment.currentProgressValue != null ? assignment.currentProgressValue : 0;
			progressCurrentText.textContent = `${currentAccumulatedValue} / ${assignment.measurableValue || '0'}`;
			progressEvidenceInfo.innerHTML = assignment.evidenceFilename
				? `<a href="/api/employee/kpis/evidence/${encodeURIComponent(assignment.evidenceFilename)}" target="_blank" class="action-link-modern">View last upload</a>`
				: 'No file uploaded';
			if (progressStatusBadge) {
				progressStatusBadge.innerHTML = getStatusBadgeMarkup(assignment.status);
			}
			progressValueInput.value = '';
			progressEvidenceInput.value = '';
			const locked = isAssignmentLocked(assignment);
			if (progressLockNotice) {
				progressLockNotice.style.display = locked ? 'block' : 'none';
			}
			progressValueInput.disabled = locked;
			progressEvidenceInput.disabled = locked;
			const submitButton = document.getElementById('kpiProgressSubmitButton');
			if (submitButton) {
				submitButton.disabled = locked;
			}
			if (locked) {
				showStatus('This KPI is locked because the due date has been reached.');
			}
			updateProgressPreview();
			if (scrollIntoView) {
				progressForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
			}
		};

		if (progressForm) {
			progressForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				if (!selectedAssignmentId) {
					showStatus('Please select a KPI from the table above');
					return;
				}
				const rawValue = progressValueInput.value.trim();
				if (!/^\d+$/.test(rawValue)) {
					showStatus('Progress value must contain digits only');
					return;
				}
				const progressValue = parseInt(rawValue, 10);
				if (Number.isNaN(progressValue) || progressValue <= 0) {
					showStatus('Progress value must be greater than 0');
					return;
				}
				if (!progressEvidenceInput.files || progressEvidenceInput.files.length === 0) {
					showStatus('Evidence upload is required');
					return;
				}

				const formData = new FormData();
				formData.append('progressValue', progressValue.toString());
				formData.append('evidence', progressEvidenceInput.files[0]);
				const res = await fetch(`/api/employee/kpis/${selectedAssignmentId}/progress?userId=${encodeURIComponent(session.userId)}`, {
					method: 'POST',
					body: formData
				});

				if (res.ok) {
					showStatus('KPI progress updated successfully');
					resetProgressForm();
					loadEmployeeKpis(searchInput?.value.trim() || '');
				} else {
					const text = await res.text();
					let errorMessage = 'Failed to update KPI progress';
					try {
						const parsed = JSON.parse(text);
						errorMessage = parsed.message || errorMessage;
					} catch (_) {
						if (text && text.trim()) {
							errorMessage = text.trim();
						}
					}
					showStatus(errorMessage);
				}
			});
		}

		resetProgressForm();
		loadEmployeeKpis();
	};

	const initEmployeeAttendance = () => {
		const form = document.getElementById('attendanceForm');
		const historyContainer = document.getElementById('attendanceHistoryContainer');
		if (!form) return;

		let employeeProfile = null;

		// Cached inputs & buttons
		const checkInInput = form.querySelector('[name="checkIn"]');
		const checkOutInput = form.querySelector('[name="checkOut"]');
		const notesInput = form.querySelector('[name="notes"]');
		const clockInBtn = document.getElementById('btnClockIn');
		const clockOutBtn = document.getElementById('btnClockOut');

		// Set today's date
		const setTodayDate = () => {
			const workDateInput = document.getElementById('workDate');
			if (workDateInput) {
				const today = new Date();
				const year = today.getFullYear();
				const month = String(today.getMonth() + 1).padStart(2, '0');
				const day = String(today.getDate()).padStart(2, '0');
				workDateInput.value = `${year}-${month}-${day}`;
			}
		};

		const getCurrentTimeHHMM = () => {
			const now = new Date();
			const hours = String(now.getHours()).padStart(2, '0');
			const minutes = String(now.getMinutes()).padStart(2, '0');
			return `${hours}:${minutes}`;
		};

		// Load employee profile and populate employee ID
		const loadEmployeeProfile = async () => {
			if (session.userId && form.userId) {
				form.userId.value = session.userId;
				const res = await api(`/api/employee/profile/${session.userId}`);
				if (res.ok) {
					employeeProfile = res.payload;
					const employeeIdDisplay = document.getElementById('employeeIdDisplay');
					if (employeeIdDisplay) {
						employeeIdDisplay.value = res.payload.employeeId || 'N/A';
					}
				}
			}
			// Always set today's date
			setTodayDate();
		};

		// Render attendance history
		const renderAttendanceHistory = async () => {
			const userId = form.userId.value || session.userId;
			if (!userId) return;

			const res = await api(`/api/employee/attendance/${userId}`);
			if (!res.ok || !historyContainer) return;

			const attendanceRecords = res.payload || [];

			if (attendanceRecords.length === 0) {
				historyContainer.innerHTML = '<p>No attendance records found</p>';
				return;
			}

			const table = document.createElement('table');
			table.innerHTML = `
				<thead>
					<tr>
						<th>Attendance ID</th>
						<th>Employee ID</th>
						<th>Work Date</th>
						<th>Check In</th>
						<th>Check Out</th>
						<th>Status</th>
						<th>Notes</th>
					</tr>
				</thead>
				<tbody>
					${attendanceRecords.map(record => `
						<tr>
							<td>${record.attendanceId || 'N/A'}</td>
							<td>${record.employeeId || employeeProfile?.employeeId || 'N/A'}</td>
							<td>${record.workDate || 'N/A'}</td>
							<td>${record.checkIn || '-'}</td>
							<td>${record.checkOut || '-'}</td>
							<td>${record.status || 'PRESENT'}</td>
							<td>${record.notes || '-'}</td>
						</tr>
					`).join('')}
				</tbody>
			`;
			historyContainer.innerHTML = '';
			historyContainer.appendChild(table);
		};

		// Helper function to validate WiFi and location before submission
		const validateBeforeSubmit = async () => {
			// Check WiFi connection
			const wifiCheck = isWifiConnection();
			if (!wifiCheck.valid) {
				showModal({
					title: 'Network Connection Error',
					message: wifiCheck.message,
					type: 'error',
					details: [
						'Please ensure you are connected to the company Wi-Fi network',
						'If you are connected but still see this error, please contact IT department',
						'Some browsers may not be able to detect network type, the system will verify on the backend'
					]
				});
				return false;
			}

			// Check location
			try {
				const position = await requireAttendanceLocation();
				return { valid: true, position };
			} catch (error) {
				const isLocationError = error.message.includes('outside the company') || 
					error.message.includes('Location accuracy') ||
					error.message.includes('不在公司指定位置') || 
					error.message.includes('定位精度不足');
				
				if (isLocationError) {
					showModal({
						title: 'Location Verification Failed',
						message: error.message,
						type: 'error',
						details: [
							'Please ensure you are within the company designated location range',
							'Please ensure location services are enabled and accuracy is sufficient',
							'If the problem persists, please contact HR department'
						]
					});
				} else {
					showModal({
						title: 'Location Permission Error',
						message: error.message,
						type: 'error',
						details: [
							'Please allow the browser to access your location information',
							'Enable location permission in browser settings',
							'Refresh the page and try again'
						]
					});
				}
				return false;
			}
		};

		// Clock In / Clock Out buttons
		if (clockInBtn && checkInInput && checkOutInput) {
			clockInBtn.addEventListener('click', async () => {
				const validation = await validateBeforeSubmit();
				if (!validation || !validation.valid) {
					return;
				}

				setTodayDate();
				checkInInput.value = getCurrentTimeHHMM();
				checkOutInput.value = '';
				if (notesInput) {
					notesInput.value = 'Clock in';
				}
				
				// Submit with position data
				await submitAttendance(validation.position);
			});
		}

		if (clockOutBtn && checkInInput && checkOutInput) {
			clockOutBtn.addEventListener('click', async () => {
				const validation = await validateBeforeSubmit();
				if (!validation || !validation.valid) {
					return;
				}

				setTodayDate();
				if (!checkInInput.value) {
					checkInInput.value = getCurrentTimeHHMM();
				}
				checkOutInput.value = getCurrentTimeHHMM();
				if (notesInput) {
					notesInput.value = 'Clock out';
				}
				
				// Submit with position data
				await submitAttendance(validation.position);
			});
		}

		// Submit attendance function
		const submitAttendance = async (position) => {
			const data = Object.fromEntries(new FormData(form));
			data.latitude = position.latitude;
			data.longitude = position.longitude;
			data.accuracyMeters = position.accuracyMeters;
			
			const res = await api('/api/employee/attendance', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(data)
			});
			
			if (res.ok) {
				showStatus('Attendance submitted successfully');
				form.reset();
				form.userId.value = session.userId;
				if (employeeProfile) {
					document.getElementById('employeeIdDisplay').value = employeeProfile.employeeId;
				}
				setTodayDate();
				await renderAttendanceHistory();
			} else {
				// Check if error is related to WiFi or location
				const errorMessage = res.payload?.message || 'Attendance submission failed';
				const statusCode = res.status || 200;
				
				// Network validation errors (403 Forbidden)
				const isNetworkError = statusCode === 403 && (
					errorMessage.includes('Wi-Fi') || 
					errorMessage.includes('WiFi') ||
					errorMessage.includes('network') || 
					errorMessage.includes('IP address') ||
					errorMessage.includes('not authorized')
				);
				
				// Location validation errors
				const isLocationError = errorMessage.includes('location') || 
					errorMessage.includes('on-site') || 
					errorMessage.includes('位置') ||
					errorMessage.includes('accuracy');

				if (isNetworkError) {
					showModal({
						title: 'Network Verification Failed',
						message: errorMessage,
						type: 'error',
						details: [
							'Your IP address is not within the allowed network range',
							'Please ensure you are connected to the company Wi-Fi network',
							'Check if your network connection is working properly',
							'If the problem persists, please contact IT department',
							`Error code: ${statusCode}`
						]
					});
				} else if (isLocationError) {
					showModal({
						title: 'Location Verification Failed',
						message: errorMessage,
						type: 'error',
						details: [
							'Please ensure you are within the company designated location range',
							'Check if location services are working properly',
							'If the problem persists, please contact HR department',
							`Error code: ${statusCode}`
						]
					});
				} else {
					showModal({
						title: 'Clock In/Out Failed',
						message: errorMessage,
						type: 'error',
						details: [
							'Please check your network connection',
							'If the problem persists, please contact administrator',
							`Error code: ${statusCode}`
						]
					});
				}
			}
		};

		// View history button (kept for compatibility if needed)
		form.addEventListener('click', async (event) => {
			if (event.target.dataset.action === 'listAttendance') {
				await renderAttendanceHistory();
				showStatus('Attendance history loaded');
			}
		});

		// Submit attendance form (fallback for direct form submission)
		form.addEventListener('submit', async (event) => {
			event.preventDefault();
			const validation = await validateBeforeSubmit();
			if (!validation || !validation.valid) {
				return;
			}
			await submitAttendance(validation.position);
		});

		// Initial load
		const initializePage = async () => {
			await loadEmployeeProfile();
			await renderAttendanceHistory(); // Auto-load attendance history
		};

		initializePage();
	};

	const initEmployeeLeave = () => {
		const form = document.getElementById('leaveForm');
		const resultBox = document.getElementById('leaveResult');
		const balanceContainer = document.getElementById('leaveBalanceContainer');
		if (!form) return;

		let employeeGender = null; // Store employee gender for validation

		// Load and display Employee ID and get gender
		const loadEmployeeId = async () => {
			if (session.userId) {
				const userIdInput = form.querySelector('[name="userId"]');
				const displayEmployeeIdInput = document.getElementById('displayEmployeeId');
				if (userIdInput) {
					userIdInput.value = session.userId;
				}
				
				// Fetch employee ID and gender from profile
				const res = await api(`/api/employee/profile/${session.userId}`);
				if (res.ok && res.payload) {
					if (displayEmployeeIdInput) {
						displayEmployeeIdInput.value = res.payload.employeeId || 'N/A';
					}
					employeeGender = res.payload.gender || null;
					// Reload leave types and balances after getting gender
					await loadLeaveTypes();
					await loadLeaveBalances();
				}
			}
		};
		loadEmployeeId();

		// Load and display leave balances
		const loadLeaveBalances = async () => {
			if (!balanceContainer || !session.userId) return;
			
			try {
				const res = await api(`/api/employee/leave-balance/${session.userId}`);
				if (res.ok && res.payload) {
					let balances = res.payload.balances || [];
					
					// Filter out gender-specific leave types
					if (employeeGender === 'Male') {
						balances = balances.filter(b => b.leaveType !== 'Maternity Leave');
					} else if (employeeGender === 'Female') {
						balances = balances.filter(b => b.leaveType !== 'Paternity Leave');
					}
					
					if (balances.length === 0) {
						balanceContainer.innerHTML = '<p style="color: #888; text-align: center; padding: 2rem;">No leave balances available. Please contact admin to initialize your leave balances.</p>';
						return;
					}

					const table = `
						<div class="table-responsive">
							<table class="modern-table">
								<thead>
									<tr>
										<th>Leave Type</th>
										<th style="text-align: center;">Total Days</th>
										<th style="text-align: center;">Used Days</th>
										<th style="text-align: center;">Remaining Days</th>
										<th style="text-align: center;">Status</th>
									</tr>
								</thead>
								<tbody>
									${balances.map(balance => {
										const isUnlimited = balance.isUnlimited;
										const totalDisplay = isUnlimited ? 'As Needed' : balance.totalDays;
										const remainingDisplay = isUnlimited ? '—' : balance.remainingDays;
										
										let statusClass = 'status-pill status-pill-secondary';
										let statusText = '—';
										if (!isUnlimited) {
											if (balance.remainingDays <= 0) {
												statusClass = 'status-pill status-pill-danger';
												statusText = 'Exhausted';
											} else if (balance.remainingDays < (balance.totalDays * 0.2)) {
												statusClass = 'status-pill status-pill-warning';
												statusText = 'Low Balance';
											} else {
												statusClass = 'status-pill status-pill-success';
												statusText = 'Available';
											}
										}
										
										return `
											<tr>
												<td><strong>${balance.leaveType}</strong></td>
												<td style="text-align: center;">${totalDisplay}</td>
												<td style="text-align: center;">${balance.usedDays}</td>
												<td style="text-align: center; font-weight: 600;">${remainingDisplay}</td>
												<td style="text-align: center;"><span class="${statusClass}">${statusText}</span></td>
											</tr>
										`;
									}).join('')}
								</tbody>
							</table>
						</div>
					`;
					
					balanceContainer.innerHTML = table;
				} else {
					const errorMsg = res.error || res.message || 'Unknown error';
					balanceContainer.innerHTML = `<p style="color: #c33; text-align: center; padding: 2rem;">Failed to load leave balances: ${errorMsg}</p>`;
					console.error('Failed to load leave balances:', res);
				}
			} catch (error) {
				balanceContainer.innerHTML = `<p style="color: #c33; text-align: center; padding: 2rem;">Failed to load leave balances: ${error.message || 'Unknown error'}</p>`;
				console.error('Error loading leave balances:', error);
			}
		};
		loadLeaveBalances();

		// Load available leave types from admin settings
		const loadLeaveTypes = async () => {
			const leaveTypeSelect = document.getElementById('leaveTypeSelect');
			if (!leaveTypeSelect) return;

			const res = await api('/api/employee/leave-types');
			if (res.ok && res.payload && Array.isArray(res.payload)) {
				// Clear existing options except the first one
				leaveTypeSelect.innerHTML = '<option value="">-- Select Leave Type --</option>';
				
				// Filter leave types based on gender
				let filteredTypes = res.payload;
				if (employeeGender === 'Male') {
					filteredTypes = res.payload.filter(type => type !== 'Maternity Leave');
				} else if (employeeGender === 'Female') {
					filteredTypes = res.payload.filter(type => type !== 'Paternity Leave');
				}
				
				// Add filtered leave types from admin settings
				filteredTypes.forEach(leaveType => {
					const option = document.createElement('option');
					option.value = leaveType;
					option.textContent = leaveType;
					leaveTypeSelect.appendChild(option);
				});

				if (filteredTypes.length === 0) {
					leaveTypeSelect.innerHTML = '<option value="">No leave types available</option>';
					leaveTypeSelect.disabled = true;
				}
			} else {
				showStatus('Failed to load leave types');
			}
		};
		loadLeaveTypes();

		// Store leave balances for alert checking
		let leaveBalances = null;

		// Function to check leave balance and show alerts
		const checkLeaveBalanceAlert = async () => {
			const alertDiv = document.getElementById('leaveBalanceAlert');
			if (!alertDiv) return;

			const leaveType = form.querySelector('[name="leaveType"]')?.value;
			const startDate = form.querySelector('[name="startDate"]')?.value;
			const endDate = form.querySelector('[name="endDate"]')?.value;

			if (!leaveType || !startDate || !endDate) {
				alertDiv.style.display = 'none';
				return;
			}

			// Load balances if not already loaded
			if (!leaveBalances && session.userId) {
				const res = await api(`/api/employee/leave-balance/${session.userId}`);
				if (res.ok && res.payload) {
					leaveBalances = res.payload.balances;
				}
			}

			if (!leaveBalances) {
				alertDiv.style.display = 'none';
				return;
			}

			// Find balance for selected leave type
			const balance = leaveBalances.find(b => b.leaveType === leaveType);
			if (!balance) {
				alertDiv.style.display = 'none';
				return;
			}

			// Calculate requested days
			const start = new Date(startDate);
			const end = new Date(endDate);
			const requestedDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;

			// Check if unlimited
			if (balance.isUnlimited) {
				alertDiv.style.display = 'none';
				return;
			}

			// Show alerts
			const totalDisplay = balance.isUnlimited ? 'As Needed' : balance.totalDays;
			if (balance.remainingDays <= 0) {
				alertDiv.style.display = 'block';
				alertDiv.className = 'leave-alert error';
				alertDiv.innerHTML = `⚠️ <strong>Alert:</strong> You have exhausted your ${leaveType} balance (Total: ${totalDisplay}).`;
			} else if (balance.remainingDays < requestedDays) {
				alertDiv.style.display = 'block';
				alertDiv.className = 'leave-alert error';
				alertDiv.innerHTML = `⚠️ <strong>Alert:</strong> Insufficient leave balance. Requested: ${requestedDays} days, Available: ${balance.remainingDays} days (Total: ${totalDisplay}).`;
			} else if (balance.remainingDays < (balance.totalDays * 0.2)) {
				alertDiv.style.display = 'block';
				alertDiv.className = 'leave-alert warning';
				alertDiv.innerHTML = `⚠️ <strong>Warning:</strong> You have only ${balance.remainingDays} days remaining for ${leaveType} (Total: ${totalDisplay}).`;
			} else {
				alertDiv.style.display = 'none';
			}
		};

		const leaveTypeSelect = document.getElementById('leaveTypeSelect');
		const startDateInput = form.querySelector('[name="startDate"]');
		const endDateInput = form.querySelector('[name="endDate"]');
		const enforceLeaveDateMin = () => {
			const today = new Date();
			const yyyy = today.getFullYear();
			const mm = String(today.getMonth() + 1).padStart(2, '0');
			const dd = String(today.getDate()).padStart(2, '0');
			const todayStr = `${yyyy}-${mm}-${dd}`;
			if (startDateInput) startDateInput.min = todayStr;
			if (endDateInput) endDateInput.min = todayStr;
		};
		enforceLeaveDateMin();

		if (leaveTypeSelect) {
			leaveTypeSelect.addEventListener('change', checkLeaveBalanceAlert);
		}
		if (startDateInput) {
			startDateInput.addEventListener('change', checkLeaveBalanceAlert);
		}
		if (endDateInput) {
			endDateInput.addEventListener('change', checkLeaveBalanceAlert);
		}

		const renderLeaveRequests = (requests) => {
			if (!resultBox) return;
			
			if (!requests || requests.length === 0) {
				resultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 2rem;">No leave requests found.</p>';
				return;
			}

			const table = `
				<div class="table-responsive">
					<table class="modern-table">
						<thead>
							<tr>
								<th>Leave ID</th>
								<th>Leave Type</th>
								<th>Start Date</th>
								<th>End Date</th>
								<th>Reason</th>
								<th>Document</th>
								<th>Status</th>
								<th>Admin Remarks</th>
							</tr>
						</thead>
						<tbody>
							${requests.map(req => {
								let statusClass = 'status-pill';
								if (req.status === 'APPROVED') {
									statusClass += ' status-pill-success';
								} else if (req.status === 'REJECTED') {
									statusClass += ' status-pill-danger';
								} else if (req.status === 'CANCELLED') {
									statusClass += ' status-pill-secondary';
								} else {
									statusClass += ' status-pill-warning';
								}
								
								const documentLink = req.supportingDocumentFilename 
									? `<a href="/api/employee/leave-documents/${req.supportingDocumentFilename}" target="_blank" class="link-button">📄 View</a>`
									: '<span style="color: var(--text-muted);">—</span>';
								return `
									<tr>
										<td>${req.leaveId || 'N/A'}</td>
										<td><strong>${req.leaveType}</strong></td>
										<td>${req.startDate}</td>
										<td>${req.endDate}</td>
										<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${req.reason}">${req.reason}</td>
										<td style="text-align: center;">${documentLink}</td>
										<td><span class="${statusClass}">${req.status}</span></td>
										<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${req.managerComment || 'Pending'}">${req.managerComment || 'Pending'}</td>
									</tr>
								`;
							}).join('')}
						</tbody>
					</table>
				</div>
			`;
			resultBox.innerHTML = table;
		};

		const loadMyLeaveRequests = async () => {
				const { userId } = Object.fromEntries(new FormData(form));
			if (!userId) return;
				const res = await api(`/api/employee/leave/${userId}`);
			if (res.ok && res.payload) {
				renderLeaveRequests(res.payload);
			}
		};

		form.addEventListener('click', async (event) => {
			if (event.target.dataset.action === 'listLeave') {
				loadMyLeaveRequests();
			}
		});

		form.addEventListener('submit', async (event) => {
			event.preventDefault();
			const formData = new FormData(form);
			
			// Validation
			if (!formData.get('userId') || !formData.get('leaveType') || !formData.get('startDate') || !formData.get('endDate') || !formData.get('reason')) {
				showStatus('Please fill in all required fields');
				return;
			}
			
			if (new Date(formData.get('endDate')) < new Date(formData.get('startDate'))) {
				showStatus('End date must be after start date');
				return;
			}

			// Validate gender-based leave types
			const leaveType = formData.get('leaveType').trim();
			if (leaveType === 'Maternity Leave') {
				if (employeeGender !== 'Female') {
					showStatus('Maternity Leave only for female employee');
					return;
				}
			} else if (leaveType === 'Paternity Leave') {
				if (employeeGender !== 'Male') {
					showStatus('Paternity Leave only for male employee');
					return;
				}
			}

			// Validate file size if file is selected
			const fileInput = document.getElementById('supportingDocument');
			if (fileInput && fileInput.files.length > 0) {
				const file = fileInput.files[0];
				if (file.size > 10 * 1024 * 1024) {
					showStatus('File size must not exceed 10MB');
					return;
				}
			}

			// Send as multipart/form-data
			const res = await fetch('/api/employee/leave', {
				method: 'POST',
				credentials: 'same-origin',
				body: formData
			});
			
			const text = await res.text();
			let payload;
			try {
				payload = text ? JSON.parse(text) : {};
			} catch {
				payload = text;
			}

			if (res.ok) {
				showStatus('Leave request submitted successfully');
				form.reset();
				// Re-populate userId and Employee ID
				loadEmployeeId();
				loadMyLeaveRequests();
				// Refresh leave balances
				leaveBalances = null;
				loadLeaveBalances();
				// Hide alert
				const alertDiv = document.getElementById('leaveBalanceAlert');
				if (alertDiv) {
					alertDiv.style.display = 'none';
				}
			} else {
				// Extract error message from Spring Boot error response
				let errorMessage = 'Leave submission failed';
				if (typeof payload === 'string') {
					errorMessage = payload;
				} else if (payload && typeof payload === 'object') {
					errorMessage = payload.message || payload.error || 'Leave submission failed';
				}
				showStatus(errorMessage);
			}
		});

		// Auto-load leave requests on page load
		loadMyLeaveRequests();
	};

	const initEmployeeOvertime = () => {
		const form = document.getElementById('overtimeForm');
		const resultBox = document.getElementById('overtimeResult');
		if (!form) return;

		// Load next overtime ID
		const loadNextOvertimeId = async () => {
			const nextIdInput = document.getElementById('nextOvertimeId');
			if (nextIdInput) {
				const res = await api('/api/employee/overtime/next-overtime-id');
				if (res.ok && res.payload) {
					nextIdInput.value = res.payload;
				}
			}
		};
		loadNextOvertimeId();

		// Load and display Employee ID
		if (session.userId) {
			const userIdInput = form.querySelector('[name="userId"]');
			if (userIdInput) {
				userIdInput.value = session.userId;
			}
		}

		// Load my overtime requests
		const loadMyOvertimeRequests = async () => {
			if (!resultBox || !session.userId) return;
			try {
				const res = await api(`/api/employee/overtime/${session.userId}`);
				if (res.ok && res.payload) {
					const requests = Array.isArray(res.payload) ? res.payload : [];
					renderOvertimeTable(requests);
				} else {
					resultBox.innerHTML = '<p style="text-align: center; color: #c33;">Failed to load overtime requests.</p>';
				}
			} catch (error) {
				console.error('Error loading overtime requests:', error);
				resultBox.innerHTML = '<p style="text-align: center; color: #c33;">Error loading overtime requests.</p>';
			}
		};

		const renderOvertimeTable = (requests) => {
			if (!resultBox) return;
			if (!Array.isArray(requests) || requests.length === 0) {
				resultBox.innerHTML = '<div class="empty-state-modern">No overtime requests found.</div>';
				return;
			}

			const table = `
				<div class="table-responsive">
					<table class="modern-table">
						<thead>
							<tr>
								<th>Overtime ID</th>
								<th>Work Date</th>
								<th>Start Time</th>
								<th>End Time</th>
								<th>Hours</th>
								<th>Reason</th>
								<th>Status</th>
								<th>Admin Remarks</th>
							</tr>
						</thead>
						<tbody>
							${requests.map(req => {
								let statusClass = 'status-pill';
								if (req.status === 'APPROVED') {
									statusClass += ' status-pill-success';
								} else if (req.status === 'REJECTED') {
									statusClass += ' status-pill-danger';
								} else if (req.status === 'CANCELLED') {
									statusClass += ' status-pill-secondary';
								} else {
									statusClass += ' status-pill-warning';
								}
								return `
									<tr>
										<td>${req.overtimeId || 'N/A'}</td>
										<td>${req.workDate || 'N/A'}</td>
										<td>${req.startTime || 'N/A'}</td>
										<td>${req.endTime || 'N/A'}</td>
										<td>${req.hours ? req.hours.toFixed(2) : 'N/A'}</td>
										<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${req.reason}">${req.reason}</td>
										<td><span class="${statusClass}">${req.status}</span></td>
										<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${req.managerComment || 'Pending'}">${req.managerComment || 'Pending'}</td>
									</tr>
								`;
							}).join('')}
						</tbody>
					</table>
				</div>
			`;
			resultBox.innerHTML = table;
		};

		// Form submission
		if (form) {
			form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(form);
				const data = {
					userId: formData.get('userId'),
					workDate: formData.get('workDate'),
					startTime: formData.get('startTime'),
					endTime: formData.get('endTime'),
					reason: formData.get('reason')
				};

				if (!data.userId || !data.workDate || !data.startTime || !data.endTime || !data.reason) {
					showStatus('Please fill in all required fields');
					return;
				}

				const res = await api('/api/employee/overtime', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(data)
				});

				if (res.ok) {
					showStatus('Overtime request submitted successfully');
					form.reset();
					loadNextOvertimeId();
					await loadMyOvertimeRequests();
				} else {
					showStatus('Failed to submit overtime request: ' + (res.message || res.payload?.message || 'Unknown error'));
				}
			});
		}

		// Load requests on page load
		loadMyOvertimeRequests();

		// Handle list button
		const listButton = document.querySelector('[data-action="listOvertime"]');
		if (listButton) {
			listButton.addEventListener('click', loadMyOvertimeRequests);
		}
	};

	const initEmployeePayroll = () => {
		const tableBody = document.getElementById('payrollTableBody');
		const emptyState = document.getElementById('payrollEmptyState');
		if (!tableBody) return;

		const renderPayroll = (records = []) => {
			if (!records.length) {
				tableBody.innerHTML = '';
				if (emptyState) emptyState.style.display = 'block';
				return;
			}
			if (emptyState) emptyState.style.display = 'none';
			tableBody.innerHTML = records.map(record => {
				// Extract year and month from periodStart or periodEnd
				let year = '';
				let month = '';
				
				// Try to extract from periodStart first
				if (record.periodStart) {
					const date = new Date(record.periodStart);
					if (!isNaN(date.getTime())) {
						year = date.getFullYear().toString();
						month = (date.getMonth() + 1).toString().padStart(2, '0');
					}
				}
				
				// If not available, try periodEnd
				if (!year || !month) {
					if (record.periodEnd) {
						const date = new Date(record.periodEnd);
						if (!isNaN(date.getTime())) {
							year = date.getFullYear().toString();
							month = (date.getMonth() + 1).toString().padStart(2, '0');
						}
					}
				}

				const viewPayslipUrl = year && month && session.userId
					? `/payslip.html?userId=${encodeURIComponent(session.userId)}&year=${year}&month=${month}`
					: '#';
				
				const canViewPayslip = year && month && session.userId;

				return `
					<tr>
						<td>${record.periodStart || 'N/A'} &mdash; ${record.periodEnd || 'N/A'}</td>
						<td>${formatCurrency(record.baseSalary)}</td>
						<td>${formatCurrency(record.bonus)}</td>
						<td>${formatCurrency(record.deductions)}</td>
						<td class="font-bold">${formatCurrency(record.netPay)}</td>
						<td>${record.notes || '—'}</td>
						<td style="text-align: center;">
							${canViewPayslip ? `
								<button class="action-btn-modern" onclick="window.open('${viewPayslipUrl}', '_blank')" title="View Payslip">
									View Payslip
								</button>
								<button class="action-btn-modern print" onclick="window.DashboardApp.printPayslip('${session.userId}', '${year}', '${month}')" title="Print Payslip">
									Print Payslip
								</button>
							` : '<span style="color: #999; font-size: 0.85rem;">N/A</span>'}
						</td>
					</tr>
				`;
			}).join('');
		};

		const formatCurrency = (value) => {
			if (value === null || value === undefined) return '—';
			const numericValue = typeof value === 'number' ? value : Number(value);
			if (Number.isNaN(numericValue)) {
				return value;
			}
			return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(numericValue);
		};

		const loadPayroll = async () => {
			if (!session.userId) {
				const ok = await loadSession();
				if (!ok || !session.userId) {
					showStatus('Session expired, please log in again');
					return;
				}
			}
			const res = await api(`/api/employee/payroll/${session.userId}`);
			if (res.ok) {
				renderPayroll(res.payload || []);
			} else {
				showStatus('Unable to load payroll');
			}
		};

		// Print payslip function
		window.DashboardApp = window.DashboardApp || {};
		window.DashboardApp.printPayslip = (userId, year, month) => {
			const url = `/payslip.html?userId=${encodeURIComponent(userId)}&year=${year}&month=${month}`;
			const printWindow = window.open(url, '_blank');
			if (printWindow) {
				printWindow.onload = () => {
					setTimeout(() => {
						printWindow.print();
					}, 500);
				};
			}
		};

		loadPayroll();
	};

	const initEmployeeAnnouncements = () => {
		const container = document.getElementById('announcementContainer');
		const emptyState = document.getElementById('announcementEmptyState');

		// Load announcements on page load
		const loadAnnouncements = async () => {
			const res = await api('/api/employee/announcements');
			if (res.ok && res.payload) {
				renderAnnouncements(res.payload);
			} else {
				if (container) container.innerHTML = '';
				if (emptyState) emptyState.style.display = 'block';
			}
		};

		// Render announcements as cards
		const renderAnnouncements = (announcements) => {
			if (!container) return;
			if (!announcements || announcements.length === 0) {
				container.innerHTML = '';
				if (emptyState) emptyState.style.display = 'block';
				return;
			}
			if (emptyState) emptyState.style.display = 'none';

			const today = new Date();
			today.setHours(0, 0, 0, 0);

			container.innerHTML = announcements.map(announcement => {
				// Handle createdAt - can be string (ISO) or Instant
				let createdDate = 'N/A';
				if (announcement.createdAt) {
					if (typeof announcement.createdAt === 'string') {
						createdDate = new Date(announcement.createdAt).toLocaleDateString('en-US', {
							year: 'numeric',
							month: 'long',
							day: 'numeric'
						});
					} else {
						createdDate = new Date(announcement.createdAt).toLocaleDateString('en-US', {
							year: 'numeric',
							month: 'long',
							day: 'numeric'
						});
					}
				}

				const expiresDate = announcement.expiresOn ? new Date(announcement.expiresOn).toLocaleDateString('en-US', {
					year: 'numeric',
					month: 'long',
					day: 'numeric'
				}) : null;
				const expiresDateObj = announcement.expiresOn ? new Date(announcement.expiresOn) : null;
				expiresDateObj?.setHours(0, 0, 0, 0);

				const isExpired = expiresDateObj && expiresDateObj < today;
				const isPinned = announcement.pinned === true;

				let statusBadge = '';
				if (isPinned) {
					statusBadge = '<span class="status-pill-modern pinned">Pinned</span>';
				} else if (isExpired) {
					statusBadge = '<span class="status-pill-modern expired">Expired</span>';
				} else {
					statusBadge = '<span class="status-pill-modern active">Active</span>';
				}

				const cardClass = isPinned ? 'announcement-card pinned' : 'announcement-card';

				return `
					<div class="${cardClass}">
						<div class="announcement-header">
							<div>
								<h3 class="announcement-title">${announcement.title || 'No Title'}</h3>
								<div class="announcement-meta">
									<span>${statusBadge}</span>
							
									<span>${createdDate}</span>
									${expiresDate ? `<span>Expires: ${expiresDate}</span>` : ''}
									
								</div>
							</div>
						</div>
						<div class="announcement-content">
							${(announcement.message || 'No message').replace(/\n/g, '<br>')}
						</div>
					</div>
				`;
			}).join('');
		};

		// Load announcements on page load
		loadAnnouncements();
	};

	const initFloatingChatbot = () => {
		if (document.getElementById('floatingChatbotLauncher')) {
			return;
		}

		// Launcher
		const launcher = document.createElement('div');
		launcher.id = 'floatingChatbotLauncher';
		launcher.className = 'chatbot-floating-launcher';
		launcher.innerHTML = `
			<div class="chatbot-floating-label">Need help?</div>
			<button class="chatbot-floating-button" type="button" aria-label="Open AI Copilot">
				<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
					<path d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z" fill="currentColor"/>
				</svg>
			</button>
		`;

		// Panel
		const panel = document.createElement('div');
		panel.id = 'floatingChatbotPanel';
		panel.className = 'chatbot-floating-panel';
		panel.innerHTML = `
			<header class="chatbot-floating-header">
				<div class="chatbot-floating-header-main">
					<div class="chatbot-floating-avatar">
						<svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
							<path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM13 17H11V15H13V17ZM13 13H11V7H13V13Z" fill="currentColor"/>
						</svg>
					</div>
					<div class="chatbot-floating-header-text">
						<h3>Bot</h3>
						<span class="chatbot-status">
							<span class="chatbot-status-dot"></span>
							Online
						</span>
					</div>
				</div>
				<button type="button" class="chatbot-floating-icon-button" data-action="close" aria-label="Close">
					<svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
						<path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
					</svg>
				</button>
			</header>
			<div class="chatbot-floating-body">
				<div id="floatingChatHistory" class="chatbot-floating-history">
					<div class="chat-message chat-message--assistant">
						<p>Hi! I can help summarise your recent attendance, leave balances, payroll, and KPIs using the records HR has on file.</p>
					</div>
				</div>
				<div id="floatingChatLoading" class="chatbot-floating-typing" style="display: none;">
					<span class="typing-dot"></span>
					<span class="typing-dot"></span>
					<span class="typing-dot"></span>
					<span>Gemini is thinking…</span>
				</div>
				<div class="chatbot-floating-quick-replies">
					<button type="button" class="quick-reply-btn" data-chat-suggestion="How many annual leave days do I still have available?">Annual leave balance</button>
					<button type="button" class="quick-reply-btn" data-chat-suggestion="Summarise my last 3 attendance records.">Recent attendance</button>
					<button type="button" class="quick-reply-btn" data-chat-suggestion="What was my latest payroll net pay?">Latest payroll</button>
					<button type="button" class="quick-reply-btn" data-chat-suggestion="List my active KPIs and their status.">My KPIs</button>
				</div>
				<div class="chatbot-floating-input">
					<textarea id="floatingChatInput" placeholder="Ask about your records..." rows="1"></textarea>
					<button id="floatingChatSend" type="button" class="send-button">
						<svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
							<path d="M2 21L23 12L2 3V10L17 12L2 14V21Z" fill="currentColor"/>
						</svg>
					</button>
				</div>
				
			</div>
		`;

		document.body.appendChild(launcher);
		document.body.appendChild(panel);

		const button = launcher.querySelector('.chatbot-floating-button');
		const label = launcher.querySelector('.chatbot-floating-label');
		const history = document.getElementById('floatingChatHistory');
		const input = document.getElementById('floatingChatInput');
		const send = document.getElementById('floatingChatSend');
		const latencyLabel = document.getElementById('floatingChatLatency');
		const loadingIndicator = document.getElementById('floatingChatLoading');
		const quickReplyButtons = panel.querySelectorAll('.chatbot-floating-quick-replies [data-chat-suggestion]');

		const togglePanel = (open) => {
			panel.classList.toggle('chatbot-floating-panel--open', open);
			if (open && input) {
				setTimeout(() => input.focus(), 100);
			}
		};

		const setLoading = (isLoading) => {
			if (loadingIndicator) {
				loadingIndicator.style.display = isLoading ? 'flex' : 'none';
			}
			if (send) {
				send.disabled = isLoading;
			}
			if (input) {
				input.disabled = isLoading;
			}
			if (quickReplyButtons.length > 0) {
				quickReplyButtons.forEach(btn => btn.disabled = isLoading);
			}
		};

		const appendMessage = (role, text) => {
			const bubble = document.createElement('div');
			const safeRole = ['assistant', 'user', 'system'].includes(role) ? role : 'assistant';
			bubble.classList.add('chat-message', `chat-message--${safeRole}`);
			const paragraph = document.createElement('p');
			paragraph.textContent = text;
			bubble.appendChild(paragraph);
			history.appendChild(bubble);
			history.scrollTop = history.scrollHeight;
		};

		// Auto-resize textarea
		if (input) {
			input.addEventListener('input', function() {
				this.style.height = 'auto';
				this.style.height = Math.min(this.scrollHeight, 120) + 'px';
			});
		}

		const setLatency = (model, latency) => {
			if (!latencyLabel) return;
			if (!latency) {
				latencyLabel.textContent = '';
				return;
			}
			const seconds = (latency / 1000).toFixed(1);
			const labelText = model || 'Local Assistant';
			latencyLabel.textContent = `${labelText} • ${seconds}s`;
		};

		const sendMessage = async () => {
			const message = (input.value || '').trim();
			if (!message) {
				showStatus('请输入一个问题');
				if (input) input.focus();
				return;
			}
			appendMessage('user', message);
			if (input) {
				input.value = '';
				input.style.height = 'auto';
			}
			setLatency(null, null);
			setLoading(true);

			try {
				const res = await api('/api/employee/chatbot', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ message })
				});

				if (res.ok && res.payload) {
					const answer = typeof res.payload === 'object' ? res.payload.answer : null;
					appendMessage('assistant', answer || 'I could not find an answer based on your records.');
					if (typeof res.payload === 'object') {
						setLatency(res.payload.model, res.payload.latencyMillis);
					}
				} else {
					const errorMessage = typeof res.payload === 'string'
						? res.payload
						: res.payload?.message || 'Unable to reach the AI assistant.';
					appendMessage('system', errorMessage);
				}
			} catch (error) {
				appendMessage('system', 'Unable to reach the AI assistant. Please try again.');
			} finally {
				setLoading(false);
			}
		};

		if (button) {
			button.addEventListener('click', () => togglePanel(!panel.classList.contains('chatbot-floating-panel--open')));
		}
		if (label) {
			label.addEventListener('click', () => togglePanel(true));
		}

		panel.addEventListener('click', (event) => {
			const action = event.target.dataset?.action;
			if (action === 'close') {
				togglePanel(false);
			}
		});

		if (send) {
			send.addEventListener('click', () => {
				sendMessage();
			});
		}
		if (input) {
			input.addEventListener('keydown', (event) => {
				if (event.key === 'Enter' && !event.shiftKey) {
					event.preventDefault();
					sendMessage();
				}
			});
		}

		quickReplyButtons.forEach(buttonEl => {
			buttonEl.addEventListener('click', () => {
				const suggestion = buttonEl.dataset.chatSuggestion || buttonEl.textContent || '';
				if (!suggestion) {
					return;
				}
				if (input) {
					input.value = suggestion;
					input.style.height = 'auto';
					input.style.height = Math.min(input.scrollHeight, 120) + 'px';
					input.focus();
				}
				// Auto-send quick reply
				setTimeout(() => {
					sendMessage();
				}, 100);
			});
		});
	};

	const initEmployeeReports = () => {
		// Attendance Report
		const generateAttendanceBtn = document.getElementById('generateAttendanceReport');
		const exportAttendanceExcelBtn = document.getElementById('exportAttendanceExcel');
		const exportAttendancePDFBtn = document.getElementById('exportAttendancePDF');
		const attendanceResultDiv = document.getElementById('attendanceReportResult');

		if (generateAttendanceBtn) {
			generateAttendanceBtn.addEventListener('click', async () => {
				const reportType = document.getElementById('attendanceReportType').value;
				const startDate = document.getElementById('attendanceStartDate').value;
				const endDate = document.getElementById('attendanceEndDate').value;
				
				if (!startDate || !endDate) {
					showStatus('Please select start and end dates');
					return;
				}
				
				if (attendanceResultDiv) {
					attendanceResultDiv.innerHTML = '<div class="loading">Generating report...</div>';
				}
				
				try {
					const params = new URLSearchParams({
						type: reportType,
						startDate: startDate,
						endDate: endDate
					});
					
					const res = await api(`/api/employee/reports/attendance?${params}`);
					if (res.ok && res.payload) {
						const report = res.payload;
						let itemsHtml = '';
						if (report.items && report.items.length > 0) {
							itemsHtml = '<table class="report-result" style="margin-top: 16px;"><thead><tr><th>Work Date</th><th>Check In</th><th>Check Out</th><th>Status</th><th>Work Hours</th><th>OT Hours</th></tr></thead><tbody>';
							report.items.forEach(item => {
								itemsHtml += `<tr>
									<td>${item.workDate || ''}</td>
									<td>${item.checkIn || ''}</td>
									<td>${item.checkOut || ''}</td>
									<td>${item.status || ''}</td>
									<td>${item.workHours != null ? item.workHours.toFixed(2) : '0.00'}</td>
									<td>${item.overtimeHours != null ? item.overtimeHours.toFixed(2) : '0.00'}</td>
								</tr>`;
							});
							itemsHtml += '</tbody></table>';
						}
						if (attendanceResultDiv) {
							attendanceResultDiv.innerHTML = `<div class="report-result">
								<h4>Report Generated Successfully</h4>
								<p><strong>Report type:</strong> ${report.reportType || reportType}</p>
								<p><strong>Period:</strong> ${report.startDate || startDate} to ${report.endDate || endDate}</p>
								<p><strong>Date Generated:</strong> ${report.dateGenerated || new Date().toISOString().split('T')[0]}</p>
								<p><strong>Total records:</strong> ${report.totalRecords || 0}</p>
								<p><strong>Total Work Hours:</strong> ${report.totalWorkHours != null ? report.totalWorkHours.toFixed(2) : '0.00'}</p>
								<p><strong>Total Overtime Hours:</strong> ${report.totalOvertimeHours != null ? report.totalOvertimeHours.toFixed(2) : '0.00'}</p>
								${itemsHtml}
							</div>`;
						}
						if (exportAttendanceExcelBtn) exportAttendanceExcelBtn.disabled = false;
						if (exportAttendancePDFBtn) exportAttendancePDFBtn.disabled = false;
					} else {
						const errorMsg = res.payload?.message || res.rawText || 'Failed to generate report';
						if (attendanceResultDiv) {
							attendanceResultDiv.innerHTML = `<div class="error">Error: ${errorMsg}</div>`;
						}
					}
				} catch (error) {
					if (attendanceResultDiv) {
						attendanceResultDiv.innerHTML = `<div class="error">Error: ${error.message}</div>`;
					}
				}
			});
		}

		// Export Attendance
		if (exportAttendanceExcelBtn) {
			exportAttendanceExcelBtn.addEventListener('click', () => {
				const reportType = document.getElementById('attendanceReportType').value;
				const startDate = document.getElementById('attendanceStartDate').value;
				const endDate = document.getElementById('attendanceEndDate').value;
				
				const params = new URLSearchParams({
					type: reportType,
					startDate: startDate,
					endDate: endDate
				});
				
				window.location.href = `/api/employee/reports/attendance/export/excel?${params}`;
			});
		}

		if (exportAttendancePDFBtn) {
			exportAttendancePDFBtn.addEventListener('click', () => {
				const reportType = document.getElementById('attendanceReportType').value;
				const startDate = document.getElementById('attendanceStartDate').value;
				const endDate = document.getElementById('attendanceEndDate').value;
				
				const params = new URLSearchParams({
					type: reportType,
					startDate: startDate,
					endDate: endDate
				});
				
				window.location.href = `/api/employee/reports/attendance/export/pdf?${params}`;
			});
		}

		// Performance Report
		const generatePerformanceBtn = document.getElementById('generatePerformanceReport');
		const exportPerformanceExcelBtn = document.getElementById('exportPerformanceExcel');
		const exportPerformancePDFBtn = document.getElementById('exportPerformancePDF');
		const performanceResultDiv = document.getElementById('performanceReportResult');

		if (generatePerformanceBtn) {
			generatePerformanceBtn.addEventListener('click', async () => {
				const year = parseInt(document.getElementById('performanceYear').value);
				
				if (performanceResultDiv) {
					performanceResultDiv.innerHTML = '<div class="loading">Generating report...</div>';
				}
				
				try {
					const params = new URLSearchParams({
						year: year.toString()
					});
					
					const res = await api(`/api/employee/reports/performance?${params}`);
					if (res.ok && res.payload) {
						const report = res.payload;
						let itemsHtml = '';
						if (report.items && report.items.length > 0) {
							itemsHtml = '<table class="report-result" style="margin-top: 16px;"><thead><tr><th>KPI ID</th><th>KPI Name</th><th>Target</th><th>Actual</th><th>Achievement %</th><th>Status</th><th>Bonus</th></tr></thead><tbody>';
							report.items.forEach(item => {
								itemsHtml += `<tr>
									<td>${item.kpiId || ''}</td>
									<td>${item.kpiName || ''}</td>
									<td>${(item.targetValue || 0).toFixed(2)}</td>
									<td>${(item.actualValue || 0).toFixed(2)}</td>
									<td>${(item.achievementPercentage || 0).toFixed(2)}%</td>
									<td>${item.status || ''}</td>
									<td>RM ${(item.bonusAmount || 0).toFixed(2)}</td>
								</tr>`;
							});
							itemsHtml += '</tbody></table>';
						}
						if (performanceResultDiv) {
							performanceResultDiv.innerHTML = `<div class="report-result">
								<h4>Report Generated Successfully</h4>
								<p><strong>Report type:</strong> Performance Analytics</p>
								<p><strong>Year:</strong> ${report.year || year}</p>
								<p><strong>Total KPIs:</strong> ${report.totalRecords || 0}</p>
								${itemsHtml}
							</div>`;
						}
						if (exportPerformanceExcelBtn) exportPerformanceExcelBtn.disabled = false;
						if (exportPerformancePDFBtn) exportPerformancePDFBtn.disabled = false;
					} else {
						const errorMsg = res.payload?.message || res.rawText || 'Failed to generate report';
						if (performanceResultDiv) {
							performanceResultDiv.innerHTML = `<div class="error">Error: ${errorMsg}</div>`;
						}
					}
				} catch (error) {
					if (performanceResultDiv) {
						performanceResultDiv.innerHTML = `<div class="error">Error: ${error.message}</div>`;
					}
				}
			});
		}

		// Export Performance
		if (exportPerformanceExcelBtn) {
			exportPerformanceExcelBtn.addEventListener('click', () => {
				const year = document.getElementById('performanceYear').value;
				const params = new URLSearchParams({ year: year });
				window.location.href = `/api/employee/reports/performance/export/excel?${params}`;
			});
		}

		if (exportPerformancePDFBtn) {
			exportPerformancePDFBtn.addEventListener('click', () => {
				const year = document.getElementById('performanceYear').value;
				const params = new URLSearchParams({ year: year });
				window.location.href = `/api/employee/reports/performance/export/pdf?${params}`;
			});
		}

		// Payroll Report
		const generatePayrollBtn = document.getElementById('generatePayrollReport');
		const exportPayrollExcelBtn = document.getElementById('exportPayrollExcel');
		const exportPayrollPDFBtn = document.getElementById('exportPayrollPDF');
		const payrollResultDiv = document.getElementById('payrollReportResult');

		if (generatePayrollBtn) {
			generatePayrollBtn.addEventListener('click', async () => {
				const period = document.getElementById('payrollReportPeriod').value;
				const month = document.getElementById('payrollMonth').value;
				const year = document.getElementById('payrollYear').value;
				const includeStatutory = document.getElementById('includeStatutory').checked;
				
				if (payrollResultDiv) {
					payrollResultDiv.innerHTML = '<div class="loading">Generating report...</div>';
				}
				
				try {
					const params = new URLSearchParams({
						period: period,
						includeStatutory: includeStatutory
					});
					
					if (period === 'monthly' && month) {
						params.append('month', month);
					} else if (period === 'annual' && year) {
						params.append('year', year);
					} else {
						showStatus('Please select month or year');
						return;
					}
					
					const res = await api(`/api/employee/reports/payroll?${params}`);
					if (res.ok && res.payload) {
						const report = res.payload;
						let itemsHtml = '';
						if (report.items && report.items.length > 0) {
							itemsHtml = '<table class="report-result" style="margin-top: 16px;"><thead><tr><th>Period</th><th>Basic Salary</th><th>Adjusted Salary</th><th>OT Pay</th><th>KPI Bonus</th><th>Benefit Bonus</th><th>Gross Pay</th><th>Deductions</th><th>Net Pay</th></tr></thead><tbody>';
							report.items.forEach(item => {
								itemsHtml += `<tr>
									<td>${report.month || report.reportType || ''}</td>
									<td>RM ${(item.basicSalary || 0).toFixed(2)}</td>
									<td>RM ${(item.adjustedBasicSalary || 0).toFixed(2)}</td>
									<td>RM ${(item.totalOtPay || 0).toFixed(2)}</td>
									<td>RM ${(item.kpiBonus || 0).toFixed(2)}</td>
									<td>RM ${(item.benefitBonus || 0).toFixed(2)}</td>
									<td>RM ${(item.grossPay || 0).toFixed(2)}</td>
									<td>RM ${(item.totalEmployeeDeductions || 0).toFixed(2)}</td>
									<td>RM ${(item.netPay || 0).toFixed(2)}</td>
								</tr>`;
							});
							itemsHtml += '</tbody></table>';
						}
						let statutoryHtml = '';
						if (report.includeStatutory && report.statutorySummary) {
							const s = report.statutorySummary;
							statutoryHtml = `<div style="margin-top: 16px; padding: 12px; background: #fff; border-radius: 8px; border: 1px solid var(--border-color);">
								<h5>Statutory Contributions Summary</h5>
								<p><strong>Total EPF (Employee):</strong> RM ${(s.totalEpfEmployee || 0).toFixed(2)}</p>
								<p><strong>Total EPF (Employer):</strong> RM ${(s.totalEpfEmployer || 0).toFixed(2)}</p>
								<p><strong>Total SOCSO (Employee):</strong> RM ${(s.totalSocsoEmployee || 0).toFixed(2)}</p>
								<p><strong>Total SOCSO (Employer):</strong> RM ${(s.totalSocsoEmployer || 0).toFixed(2)}</p>
								<p><strong>Total EIS (Employee):</strong> RM ${(s.totalEisEmployee || 0).toFixed(2)}</p>
								<p><strong>Total EIS (Employer):</strong> RM ${(s.totalEisEmployer || 0).toFixed(2)}</p>
								<p><strong>Total PCB:</strong> RM ${(s.totalPcb || 0).toFixed(2)}</p>
							</div>`;
						}
						if (payrollResultDiv) {
							payrollResultDiv.innerHTML = `<div class="report-result">
								<h4>Report Generated Successfully</h4>
								<p><strong>Report type:</strong> ${period === 'monthly' ? 'Monthly' : 'Annual'} Payroll</p>
								<p><strong>Period:</strong> ${report.month || report.reportType || (period === 'monthly' ? month : year)}</p>
								<p><strong>Total records:</strong> ${report.totalEmployees || 0}</p>
								${itemsHtml}
								${statutoryHtml}
							</div>`;
						}
						if (exportPayrollExcelBtn) exportPayrollExcelBtn.disabled = false;
						if (exportPayrollPDFBtn) exportPayrollPDFBtn.disabled = false;
					} else {
						const errorMsg = res.payload?.message || res.rawText || 'Failed to generate report';
						if (payrollResultDiv) {
							payrollResultDiv.innerHTML = `<div class="error">Error: ${errorMsg}</div>`;
						}
					}
				} catch (error) {
					if (payrollResultDiv) {
						payrollResultDiv.innerHTML = `<div class="error">Error: ${error.message}</div>`;
					}
				}
			});
		}

		// Export Payroll
		if (exportPayrollExcelBtn) {
			exportPayrollExcelBtn.addEventListener('click', () => {
				const period = document.getElementById('payrollReportPeriod').value;
				const month = document.getElementById('payrollMonth').value;
				const year = document.getElementById('payrollYear').value;
				const includeStatutory = document.getElementById('includeStatutory').checked;
				
				const params = new URLSearchParams({
					period: period,
					includeStatutory: includeStatutory
				});
				
				if (period === 'monthly' && month) {
					params.append('month', month);
				} else if (period === 'annual' && year) {
					params.append('year', year);
				}
				
				window.location.href = `/api/employee/reports/payroll/export/excel?${params}`;
			});
		}

		if (exportPayrollPDFBtn) {
			exportPayrollPDFBtn.addEventListener('click', () => {
				const period = document.getElementById('payrollReportPeriod').value;
				const month = document.getElementById('payrollMonth').value;
				const year = document.getElementById('payrollYear').value;
				const includeStatutory = document.getElementById('includeStatutory').checked;
				
				const params = new URLSearchParams({
					period: period,
					includeStatutory: includeStatutory
				});
				
				if (period === 'monthly' && month) {
					params.append('month', month);
				} else if (period === 'annual' && year) {
					params.append('year', year);
				}
				
				window.location.href = `/api/employee/reports/payroll/export/pdf?${params}`;
			});
		}
	};

	const initEmployeeChatbot = () => {
		const history = document.getElementById('chatHistory');
		const form = document.getElementById('chatInputForm');
		const input = document.getElementById('chatInput');
		const sendButton = document.getElementById('chatSendButton');
		const latencyLabel = document.getElementById('chatLatency');
		const loadingIndicator = document.getElementById('chatLoadingIndicator');
		const contextList = document.getElementById('chatContextList');
		const suggestionButtons = document.querySelectorAll('[data-chat-suggestion]');

		if (!history || !form || !input) {
			return;
		}

		const scrollToBottom = () => {
			history.scrollTop = history.scrollHeight;
		};

		const appendMessage = (role, text) => {
			const bubble = document.createElement('div');
			const safeRole = ['assistant', 'user', 'system'].includes(role) ? role : 'assistant';
			bubble.classList.add('chat-message', `chat-message--${safeRole}`);
			const paragraph = document.createElement('p');
			paragraph.textContent = text;
			bubble.appendChild(paragraph);
			history.appendChild(bubble);
			scrollToBottom();
		};

		const renderContextItems = (items = []) => {
			if (!contextList) return;
			contextList.innerHTML = '';
			if (!items.length) {
				const empty = document.createElement('li');
				empty.className = 'chat-context-empty';
				empty.textContent = 'No employee records were returned.';
				contextList.appendChild(empty);
				return;
			}
			items.forEach(item => {
				const li = document.createElement('li');
				const label = document.createElement('span');
				label.textContent = item?.label || item?.type || 'Context';
				const detail = document.createElement('strong');
				detail.textContent = item?.details || '—';
				li.appendChild(label);
				li.appendChild(detail);
				contextList.appendChild(li);
			});
		};

		const setLatency = (model, latency) => {
			if (!latencyLabel) return;
			if (!latency) {
				latencyLabel.textContent = '';
				return;
			}
			const seconds = (latency / 1000).toFixed(1);
			const label = model || 'Local Assistant';
			latencyLabel.textContent = `${label} • ${seconds}s`;
		};

		const setLoading = (loading) => {
			if (sendButton) {
				sendButton.disabled = loading;
			}
			if (loadingIndicator) {
				loadingIndicator.style.display = loading ? 'flex' : 'none';
			}
			form.classList.toggle('chat-input-bar--loading', loading);
		};

		const sendMessage = async () => {
			const message = input.value.trim();
			if (!message) {
				showStatus('请输入一个问题');
				input.focus();
				return;
			}

			appendMessage('user', message);
			input.value = '';
			setLatency(null, null);
			setLoading(true);

			const res = await api('/api/employee/chatbot', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ message })
			});

			setLoading(false);

			if (res.ok && res.payload) {
				const answer = typeof res.payload === 'object' ? res.payload.answer : null;
				appendMessage('assistant', answer || 'I could not find an answer based on your records.');
				if (typeof res.payload === 'object') {
					renderContextItems(res.payload.context || []);
					setLatency(res.payload.model, res.payload.latencyMillis);
				}
			} else {
				const errorMessage = typeof res.payload === 'string'
					? res.payload
					: res.payload?.message || 'Unable to reach the AI assistant.';
				appendMessage('system', errorMessage);
			}
		};

		form.addEventListener('submit', (event) => {
			event.preventDefault();
			sendMessage();
		});

		if (sendButton) {
			sendButton.addEventListener('click', (event) => {
				event.preventDefault();
				sendMessage();
			});
		}

		input.addEventListener('keydown', (event) => {
			if (event.key === 'Enter' && !event.shiftKey) {
				event.preventDefault();
				sendMessage();
			}
		});

		suggestionButtons.forEach(button => {
			button.addEventListener('click', () => {
				const suggestion = button.dataset.chatSuggestion || button.textContent || '';
				input.value = suggestion;
				input.focus();
			});
		});
	};

	const initAdminAttendance = () => {
		const createForm = document.getElementById('adminAttendanceCreateForm');
		const editForm = document.getElementById('adminAttendanceEditForm');
		const fetchButton = document.getElementById('adminAttendanceFetch');
		const deleteButton = document.getElementById('adminAttendanceDelete');
		const clearButton = document.getElementById('adminAttendanceClearForm');
		const filterEmployee = document.getElementById('adminAttendanceFilterEmployee');
		const filterDateFrom = document.getElementById('adminAttendanceDateFrom');
		const filterDateTo = document.getElementById('adminAttendanceDateTo');
		const createEmployeeSelect = document.getElementById('createEmployeeSelect');
		const createEmployeeNameInput = document.getElementById('createEmployeeName');

		let attendanceData = [];
		let activeEmployees = [];

		// Modal functions
		const openEditModal = () => {
			const modal = document.getElementById('editAttendanceModal');
			if (modal) {
				modal.classList.add('show');
				document.body.style.overflow = 'hidden';
			}
		};

		const closeEditModal = () => {
			const modal = document.getElementById('editAttendanceModal');
			if (modal) {
				modal.classList.remove('show');
				document.body.style.overflow = '';
			}
		};

		const openAddModal = async () => {
			const modal = document.getElementById('addAttendanceModal');
			if (modal) {
				// Load next attendance ID and active employees when opening modal
				await loadNextAttendanceId();
				await loadActiveEmployees();
				// Set default work date to today
				const workDateInput = document.querySelector('#adminAttendanceCreateForm input[name="workDate"]');
				if (workDateInput) {
					const today = new Date().toISOString().split('T')[0];
					workDateInput.value = today;
				}
				modal.classList.add('show');
				document.body.style.overflow = 'hidden';
			}
		};

		const closeAddModal = () => {
			const modal = document.getElementById('addAttendanceModal');
			if (modal) {
				modal.classList.remove('show');
				document.body.style.overflow = '';
				// Reset form when closing
				if (createForm) {
					createForm.reset();
					resetCreateEmployeeSelection();
				}
			}
		};

		// Close modal handlers
		const closeEditBtn = document.getElementById('closeEditAttendanceModal');
		if (closeEditBtn) {
			closeEditBtn.addEventListener('click', closeEditModal);
		}

		const closeAddBtn = document.getElementById('closeAddAttendanceModal');
		if (closeAddBtn) {
			closeAddBtn.addEventListener('click', closeAddModal);
		}

		// Open Add Attendance modal
		const addAttendanceBtn = document.getElementById('adminAddAttendance');
		if (addAttendanceBtn) {
			addAttendanceBtn.addEventListener('click', openAddModal);
		}

		// Close modal when clicking outside
		document.addEventListener('click', (e) => {
			const editModal = document.getElementById('editAttendanceModal');
			if (e.target === editModal) {
				closeEditModal();
			}
			const addModal = document.getElementById('addAttendanceModal');
			if (e.target === addModal) {
				closeAddModal();
			}
		});

		// Load next attendance ID
		const loadNextAttendanceId = async () => {
			const res = await api('/api/admin/attendance/next-attendance-id');
			if (res.ok) {
				const nextId = document.getElementById('nextAttendanceId');
				if (nextId) nextId.value = res.payload;
			}
		};

		const syncCreateEmployeeName = () => {
			if (!createEmployeeSelect || !createEmployeeNameInput) return;
			const selectedOption = createEmployeeSelect.options[createEmployeeSelect.selectedIndex];
			createEmployeeNameInput.value = selectedOption?.dataset?.name || '';
		};

		const resetCreateEmployeeSelection = () => {
			if (createEmployeeSelect) {
				createEmployeeSelect.value = '';
			}
			syncCreateEmployeeName();
		};

		const loadActiveEmployees = async () => {
			if (!createEmployeeSelect) return;
			const res = await api('/api/admin/users');
			if (res.ok && Array.isArray(res.payload)) {
				activeEmployees = res.payload.filter(user => user.active && user.role === 'EMPLOYEE' && user.employeeId);
				createEmployeeSelect.innerHTML = '<option value="">Select an active employee</option>';
				activeEmployees.forEach(user => {
					const option = document.createElement('option');
					option.value = user.employeeId;
					option.textContent = `${user.fullName || user.username || user.employeeId} (${user.employeeId})`;
					option.dataset.name = user.fullName || user.username || user.employeeId;
					createEmployeeSelect.appendChild(option);
				});
				resetCreateEmployeeSelection();
			} else {
				showStatus('Failed to load active employees.');
			}
		};

		// Fetch employee name by employee ID
		const fetchEmployeeName = async (employeeId, targetElementId) => {
			const targetElement = document.getElementById(targetElementId);
			if (!targetElement) return;

			if (!employeeId || employeeId.trim() === '') {
				targetElement.value = '';
				return;
			}

			const res = await api(`/api/admin/users/by-employee-id/${employeeId}`);
			if (res.ok) {
				targetElement.value = res.payload.fullName || 'N/A';
			} else {
				targetElement.value = 'Employee not found';
			}
		};

		// Load and render attendance table
		const renderAttendanceTable = async () => {
				const res = await api('/api/admin/attendance');
			if (res.ok) {
				attendanceData = res.payload || [];
				
				// Apply filters
				let filtered = [...attendanceData];
				
				const searchFilter = filterEmployee?.value?.trim().toUpperCase();
				if (searchFilter) {
					filtered = filtered.filter(a => {
						const attendanceId = (a.attendanceId || '').toUpperCase();
						const employeeId = (a.employeeId || '').toUpperCase();
						const employeeName = (a.employeeName || '').toUpperCase();
						const status = (a.status || '').toUpperCase();
						
						return attendanceId.includes(searchFilter) ||
						       employeeId.includes(searchFilter) ||
						       employeeName.includes(searchFilter) ||
						       status.includes(searchFilter);
					});
				}
				
				const dateFrom = filterDateFrom?.value;
				const dateTo = filterDateTo?.value;
				if (dateFrom || dateTo) {
					filtered = filtered.filter(a => {
						if (!a.workDate) return false;
						const workDate = new Date(a.workDate);
						if (dateFrom && workDate < new Date(dateFrom)) return false;
						if (dateTo && workDate > new Date(dateTo)) return false;
						return true;
					});
				}

				const container = document.getElementById('adminAttendanceTableContainer');
				if (!container) return;

				if (filtered.length === 0) {
					container.innerHTML = '<p>No attendance records found</p>';
					return;
				}

				const table = document.createElement('table');
				table.className = 'table-modern';
				table.innerHTML = `
					<thead>
						<tr>
							<th>
								<input type="checkbox" id="adminAttendanceSelectAll" title="Select All" style="width: 18px; height: 18px; cursor: pointer; accent-color: #2f4ceb;">
							</th>
							<th>Employee ID</th>
							<th>Employee Name</th>
							<th>Work Date</th>
							<th>Check In</th>
							<th>Check Out</th>
							<th>Status</th>
							<th>Notes</th>
							<th>Actions</th>
						</tr>
					</thead>
					<tbody>
						${filtered.map(a => `
							<tr>
								<td>
									<input type="checkbox" class="attendance-row-checkbox" data-attendance-id="${a.attendanceId}" style="width: 18px; height: 18px; cursor: pointer; accent-color: #2f4ceb;">
								</td>
								<td>${a.employeeId || 'N/A'}</td>
								<td>${a.employeeName || 'N/A'}</td>
								<td>${a.workDate ? new Date(a.workDate).toLocaleDateString() : 'N/A'}</td>
								<td>${a.checkIn || '-'}</td>
								<td>${a.checkOut || '-'}</td>
								<td>
									<span class="status-pill-modern ${(a.status || '').toLowerCase().replace('_', '-')}">${a.status ? a.status.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase()) : 'N/A'}</span>
								</td>
								<td>${a.notes || '-'}</td>
								<td>
									<button class="action-btn-modern" data-action="edit-attendance" data-id="${a.attendanceId}">Edit</button>
								</td>
							</tr>
						`).join('')}
					</tbody>
				`;
				container.innerHTML = '';
				container.appendChild(table);

				// Handle select all checkbox
				const selectAllCheckbox = document.getElementById('adminAttendanceSelectAll');
				if (selectAllCheckbox) {
					selectAllCheckbox.addEventListener('change', (e) => {
						const checkboxes = container.querySelectorAll('.attendance-row-checkbox');
						checkboxes.forEach(cb => {
							cb.checked = e.target.checked;
						});
						updateAttendanceDeleteButton();
					});
				}

				// Handle individual checkboxes
				container.querySelectorAll('.attendance-row-checkbox').forEach(checkbox => {
					checkbox.addEventListener('change', () => {
						updateAttendanceSelectAllState();
						updateAttendanceDeleteButton();
					});
				});

				// Attach edit handlers
				container.querySelectorAll('[data-action="edit-attendance"]').forEach(btn => {
					btn.addEventListener('click', async () => {
						const attendanceId = btn.getAttribute('data-id');
						if (!attendanceId) {
							showStatus('Attendance ID not found');
							return;
						}
						
						// Get fresh reference to the modal form (ensure we get the one in the modal)
						const editModal = document.getElementById('editAttendanceModal');
						if (!editModal) {
							showStatus('Edit modal not found');
							return;
						}
						const modalEditForm = editModal.querySelector('#adminAttendanceEditForm');
						if (!modalEditForm) {
							showStatus('Edit form not found in modal');
							return;
						}
						
						// Show loading state
						showStatus('Loading attendance record...');
						
						// Fetch the full record from the database
						try {
							const res = await api(`/api/admin/attendance/${attendanceId}`);
							if (!res.ok) {
								showStatus(res.message || 'Attendance record not found');
								return;
							}
							
							const record = res.payload;
							console.log('Loaded attendance record:', record); // Debug log
							
							// Set attendance ID in hidden field (try multiple ways to find it)
							let editAttendanceIdInput = document.getElementById('editAttendanceId');
							if (!editAttendanceIdInput && editModal) {
								editAttendanceIdInput = editModal.querySelector('#editAttendanceId');
							}
							if (!editAttendanceIdInput && modalEditForm) {
								editAttendanceIdInput = modalEditForm.querySelector('#editAttendanceId');
							}
							if (editAttendanceIdInput) {
								editAttendanceIdInput.value = record.attendanceId || attendanceId || '';
								console.log('Set editAttendanceId to:', editAttendanceIdInput.value); // Debug log
							} else {
								console.error('editAttendanceId input not found in modal');
							}
							
							// Populate Employee ID
							const employeeIdInput = modalEditForm.querySelector('[name="employeeId"]') || document.getElementById('editEmployeeId');
							if (employeeIdInput) {
								employeeIdInput.value = record.employeeId || '';
								console.log('Set employeeId:', record.employeeId);
							} else {
								console.error('Employee ID input not found');
							}
							
							// Populate Employee Name - search within modal first, then document
							let editEmployeeNameInput = editModal.querySelector('#editEmployeeName') || document.getElementById('editEmployeeName');
							if (!editEmployeeNameInput) {
								// Try finding it in the form
								editEmployeeNameInput = modalEditForm.querySelector('#editEmployeeName');
							}
							
							if (editEmployeeNameInput) {
								// Set initial value from record if available
								if (record.employeeName && record.employeeName !== 'N/A' && record.employeeName.trim()) {
									editEmployeeNameInput.value = record.employeeName;
									console.log('Set employeeName from record:', record.employeeName);
								} else {
									editEmployeeNameInput.value = 'Loading...';
								}
								
								// Always fetch employee name from employee ID to ensure it's current
								if (record.employeeId && record.employeeId.trim()) {
									try {
										const employeeRes = await api(`/api/admin/users/by-employee-id/${record.employeeId.trim()}`);
										console.log('Employee API response:', employeeRes); // Debug log
										if (employeeRes.ok && employeeRes.payload && employeeRes.payload.fullName) {
											editEmployeeNameInput.value = employeeRes.payload.fullName;
											console.log('Fetched employeeName from API:', employeeRes.payload.fullName);
										} else {
											// Keep the record value or set to N/A
											if (!record.employeeName || record.employeeName === 'N/A' || !record.employeeName.trim()) {
												editEmployeeNameInput.value = 'N/A';
											} else {
												editEmployeeNameInput.value = record.employeeName;
											}
											console.log('Employee name not found in API response, using record value');
										}
									} catch (err) {
										console.error('Failed to fetch employee name:', err);
										// Keep the record value or set to N/A
										if (!record.employeeName || record.employeeName === 'N/A' || !record.employeeName.trim()) {
											editEmployeeNameInput.value = 'N/A';
										} else {
											editEmployeeNameInput.value = record.employeeName;
										}
									}
								} else {
									editEmployeeNameInput.value = record.employeeName || 'N/A';
								}
							} else {
								console.error('Employee Name input not found in modal');
							}
							
							// Populate Work Date (format: YYYY-MM-DD for date input)
							const workDateInput = modalEditForm.querySelector('[name="workDate"]');
							if (workDateInput) {
								// Ensure date is in YYYY-MM-DD format
								let workDate = record.workDate || '';
								if (workDate) {
									if (typeof workDate === 'string' && workDate.includes('T')) {
										workDate = workDate.split('T')[0]; // Extract date part if ISO format
									} else if (workDate && typeof workDate === 'object' && workDate.year) {
										// Handle LocalDate object format
										const year = workDate.year || workDate[0];
										const month = String(workDate.monthValue || workDate[1] || workDate.month).padStart(2, '0');
										const day = String(workDate.dayOfMonth || workDate[2] || workDate.day).padStart(2, '0');
										workDate = `${year}-${month}-${day}`;
									}
								}
								workDateInput.value = workDate;
								console.log('Set workDate:', workDate);
							} else {
								console.error('Work Date input not found');
							}
							
							// Populate Check In Time (format: HH:mm for time input)
							const checkInInput = modalEditForm.querySelector('[name="checkIn"]');
							if (checkInInput) {
								// Handle both HH:mm:ss and HH:mm formats
								let checkInTime = record.checkIn || '';
								if (checkInTime) {
									if (typeof checkInTime === 'string') {
										// Remove seconds if present (HH:mm:ss -> HH:mm)
										if (checkInTime.length > 5) {
											checkInTime = checkInTime.substring(0, 5);
										}
									} else if (typeof checkInTime === 'object' && checkInTime.hour !== undefined) {
										// Handle LocalTime object format
										const hour = String(checkInTime.hour || checkInTime[0]).padStart(2, '0');
										const minute = String(checkInTime.minute || checkInTime[1]).padStart(2, '0');
										checkInTime = `${hour}:${minute}`;
									}
								}
								checkInInput.value = checkInTime;
								console.log('Set checkIn:', checkInTime);
							} else {
								console.error('Check In input not found');
							}
							
							// Populate Check Out Time (format: HH:mm for time input)
							const checkOutInput = modalEditForm.querySelector('[name="checkOut"]');
							if (checkOutInput) {
								// Handle both HH:mm:ss and HH:mm formats
								let checkOutTime = record.checkOut || '';
								if (checkOutTime) {
									if (typeof checkOutTime === 'string') {
										// Remove seconds if present (HH:mm:ss -> HH:mm)
										if (checkOutTime.length > 5) {
											checkOutTime = checkOutTime.substring(0, 5);
										}
									} else if (typeof checkOutTime === 'object' && checkOutTime.hour !== undefined) {
										// Handle LocalTime object format
										const hour = String(checkOutTime.hour || checkOutTime[0]).padStart(2, '0');
										const minute = String(checkOutTime.minute || checkOutTime[1]).padStart(2, '0');
										checkOutTime = `${hour}:${minute}`;
									}
								}
								checkOutInput.value = checkOutTime;
								console.log('Set checkOut:', checkOutTime);
							} else {
								console.error('Check Out input not found');
							}
							
							// Populate Status (ensure it matches enum value)
							const statusSelect = modalEditForm.querySelector('[name="status"]');
							if (statusSelect) {
								const statusValue = record.status || 'PRESENT';
								statusSelect.value = statusValue;
								console.log('Set status:', statusValue);
							} else {
								console.error('Status select not found');
							}
							
							// Populate Notes
							const notesTextarea = modalEditForm.querySelector('[name="notes"]');
							if (notesTextarea) {
								notesTextarea.value = record.notes || '';
								console.log('Set notes:', record.notes);
							} else {
								console.error('Notes textarea not found');
							}
							
							// Open the modal first, then ensure fields are set
							openEditModal();
							
							// Small delay to ensure modal is visible and DOM is ready
							setTimeout(() => {
								// Double-check employee name is set (in case modal opening cleared it)
								const finalEmployeeNameInput = document.getElementById('editEmployeeName');
								if (finalEmployeeNameInput && (!finalEmployeeNameInput.value || finalEmployeeNameInput.value === 'Loading...')) {
									// Re-fetch if needed
									if (record.employeeId) {
										api(`/api/admin/users/by-employee-id/${record.employeeId.trim()}`).then(employeeRes => {
											if (employeeRes.ok && employeeRes.payload && employeeRes.payload.fullName) {
												finalEmployeeNameInput.value = employeeRes.payload.fullName;
											} else if (record.employeeName && record.employeeName !== 'N/A') {
												finalEmployeeNameInput.value = record.employeeName;
											} else {
												finalEmployeeNameInput.value = 'N/A';
											}
										}).catch(err => {
											console.error('Failed to fetch employee name on retry:', err);
											if (record.employeeName && record.employeeName !== 'N/A') {
												finalEmployeeNameInput.value = record.employeeName;
											} else {
												finalEmployeeNameInput.value = 'N/A';
											}
										});
									}
								}
								showStatus('Attendance record loaded');
							}, 200);
						} catch (error) {
							console.error('Error loading attendance record:', error);
							showStatus('Failed to load attendance record: ' + error.message);
						}
					});
				});
			}
		};

		if (createEmployeeSelect) {
			createEmployeeSelect.addEventListener('change', () => {
				syncCreateEmployeeName();
			});
		}

		// Add event listener for edit form employee ID input
		const editEmployeeIdInput = document.getElementById('editEmployeeId');
		if (editEmployeeIdInput) {
			editEmployeeIdInput.addEventListener('blur', async (event) => {
				await fetchEmployeeName(event.target.value, 'editEmployeeName');
			});
		}

		// Create attendance
		if (createForm) {
			createForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(createForm);
				const payload = {
					employeeId: formData.get('employeeId'),
					workDate: formData.get('workDate'),
					checkIn: formData.get('checkIn') || null,
					checkOut: formData.get('checkOut') || null,
					status: formData.get('status'),
					notes: formData.get('notes') || ''
				};

				// Validation
				if (!payload.employeeId || payload.employeeId.trim() === '') {
					showStatus('Employee ID is required');
					return;
				}
				if (!payload.workDate) {
					showStatus('Work date is required');
					return;
				}

				const res = await api('/api/admin/attendance', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});

				if (res.ok) {
					showStatus('Attendance record created successfully', 'success');
					createForm.reset();
					resetCreateEmployeeSelection();
					await loadNextAttendanceId();
					closeAddModal(); // Close modal after successful creation
					await renderAttendanceTable();
				} else {
					const errorMsg = res.error || res.message || res.payload?.message || 'Failed to create attendance record';
					showStatus(errorMsg, 'error');
				}
			});
		}

		// Clear form
		if (clearButton) {
			clearButton.addEventListener('click', async () => {
				createForm.reset();
				resetCreateEmployeeSelection();
				await loadNextAttendanceId();
				showStatus('Form cleared');
			});
		}

		// Fetch attendance for editing
		if (fetchButton) {
			fetchButton.addEventListener('click', async () => {
				const editAttendanceIdInput = document.getElementById('editAttendanceId');
				const attendanceId = editAttendanceIdInput ? editAttendanceIdInput.value.trim() : '';
				if (!attendanceId) {
					showStatus('Please enter an Attendance ID');
					return;
				}

				const res = await api(`/api/admin/attendance/${attendanceId}`);
				if (res.ok) {
					const record = res.payload;
					editForm.employeeId.value = record.employeeId || '';
					document.getElementById('editEmployeeName').value = record.employeeName || 'N/A';
					editForm.workDate.value = record.workDate || '';
					editForm.checkIn.value = record.checkIn || '';
					editForm.checkOut.value = record.checkOut || '';
					editForm.status.value = record.status || 'PRESENT';
					editForm.notes.value = record.notes || '';
					showStatus('Attendance record loaded');
				} else {
					showStatus(res.message || 'Attendance record not found');
				}
			});
		}

		// Update attendance - attach to modal form and submit button
		const editModal = document.getElementById('editAttendanceModal');
		if (editModal) {
			const modalEditForm = editModal.querySelector('#adminAttendanceEditForm');
			const updateButton = editModal.querySelector('button[type="submit"]') || editModal.querySelector('button[form="adminAttendanceEditForm"]');
			
			// Handler function for update
			const handleUpdate = async (event) => {
				if (event) {
					event.preventDefault();
					event.stopPropagation();
				}
					
				console.log('Update triggered'); // Debug log
					
					const editAttendanceIdInput = document.getElementById('editAttendanceId');
					const attendanceId = editAttendanceIdInput ? editAttendanceIdInput.value.trim() : '';
					if (!attendanceId) {
						showStatus('Attendance ID is required');
						return;
					}

					// Get form data from the modal form
					const formData = new FormData(modalEditForm);
					const employeeId = formData.get('employeeId');
					const workDate = formData.get('workDate');
					const checkIn = formData.get('checkIn');
					const checkOut = formData.get('checkOut');
					const status = formData.get('status');
					const notes = formData.get('notes');
					
					console.log('Form values:', { employeeId, workDate, checkIn, checkOut, status, notes }); // Debug log
					
					// Validate required fields
					if (!employeeId || !employeeId.trim()) {
						showStatus('Employee ID is required');
						return;
					}
					if (!workDate || !workDate.trim()) {
						showStatus('Work Date is required');
						return;
					}
					if (!status || !status.trim()) {
						showStatus('Status is required');
						return;
					}
					
					const payload = {
						employeeId: employeeId.trim(),
						workDate: workDate.trim(),
						checkIn: checkIn && checkIn.trim() ? checkIn.trim() : null,
						checkOut: checkOut && checkOut.trim() ? checkOut.trim() : null,
						status: status.trim(),
						notes: notes ? notes.trim() : ''
					};

					console.log('Updating attendance with payload:', payload); // Debug log
					console.log('Attendance ID:', attendanceId); // Debug log
					showStatus('Updating attendance record...');

					try {
						const res = await api(`/api/admin/attendance/${attendanceId}`, {
							method: 'PUT',
							headers: { 'Content-Type': 'application/json' },
							body: JSON.stringify(payload)
						});

						console.log('Update response:', res); // Debug log

						if (res.ok) {
							showStatus('Attendance record updated successfully');
							modalEditForm.reset();
							await renderAttendanceTable();
							closeEditModal();
						} else {
							const errorMsg = res.message || res.payload?.message || res.error || res.rawText || 'Failed to update attendance record';
							console.error('Update failed:', res); // Debug log
							showStatus('Update failed: ' + errorMsg);
						}
					} catch (error) {
						console.error('Update error:', error);
						showStatus('Update failed: ' + error.message);
					}
				};
			
			// Attach to form submit
			if (modalEditForm) {
				modalEditForm.addEventListener('submit', handleUpdate);
			}
			
			// Also attach to update button click (in case form submit doesn't work)
			if (updateButton) {
				updateButton.addEventListener('click', (e) => {
					e.preventDefault();
					handleUpdate(e);
				});
			}
		}

		// Delete attendance - attach to modal delete button
		const modalDeleteButton = document.getElementById('adminAttendanceDelete');
		if (modalDeleteButton) {
			modalDeleteButton.addEventListener('click', async () => {
				const editAttendanceIdInput = document.getElementById('editAttendanceId');
				const attendanceId = editAttendanceIdInput ? editAttendanceIdInput.value.trim() : '';
				if (!attendanceId) {
					showStatus('Attendance ID is required');
					return;
				}

				if (!confirm(`Are you sure you want to delete attendance record ${attendanceId}? This action cannot be undone.`)) {
					return;
				}

				console.log('Deleting attendance:', attendanceId); // Debug log
				showStatus('Deleting attendance record...');

				try {
					const res = await api(`/api/admin/attendance/${attendanceId}`, {
						method: 'DELETE'
					});

					console.log('Delete response:', res); // Debug log

					// DELETE requests typically return 204 No Content, which is considered ok
					if (res.ok || res.status === 204) {
						showStatus('Attendance record deleted successfully');
						await renderAttendanceTable();
						// Refresh next attendance ID after deletion
						await loadNextAttendanceId();
						closeEditModal();
					} else {
						const errorMsg = res.message || res.payload?.message || res.error || res.rawText || 'Failed to delete attendance record';
						console.error('Delete failed:', res); // Debug log
						showStatus('Delete failed: ' + errorMsg);
					}
				} catch (error) {
					console.error('Delete error:', error);
					showStatus('Delete failed: ' + error.message);
				}
			});
		}

		// Filter listeners
		if (filterEmployee) {
			filterEmployee.addEventListener('input', renderAttendanceTable);
		}
		if (filterDateFrom) {
			filterDateFrom.addEventListener('change', renderAttendanceTable);
		}
		if (filterDateTo) {
			filterDateTo.addEventListener('change', renderAttendanceTable);
		}

		// Mark absent button
		const markAbsentButton = document.getElementById('adminMarkAbsent');
		if (markAbsentButton) {
			markAbsentButton.addEventListener('click', async () => {
				if (!confirm('This will mark all employees who have not submitted attendance today as ABSENT. Continue?')) {
					return;
				}
				
				const res = await api('/api/admin/attendance/mark-absent-today', {
					method: 'POST'
				});
				
				if (res.ok) {
					showStatus('Successfully marked absent employees for today');
					await renderAttendanceTable();
				} else {
					showStatus(res.message || 'Failed to mark absent employees');
				}
			});
		}

		// Update select all checkbox state
		const updateAttendanceSelectAllState = () => {
			const selectAllCheckbox = document.getElementById('adminAttendanceSelectAll');
			if (!selectAllCheckbox) return;
			const checkboxes = document.querySelectorAll('.attendance-row-checkbox');
			const checkedCount = document.querySelectorAll('.attendance-row-checkbox:checked').length;
			
			if (checkboxes.length === 0) {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = false;
			} else if (checkedCount === 0) {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = false;
			} else if (checkedCount === checkboxes.length) {
				selectAllCheckbox.checked = true;
				selectAllCheckbox.indeterminate = false;
			} else {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = true;
			}
		};

		// Update delete button visibility
		const updateAttendanceDeleteButton = () => {
			const deleteButton = document.getElementById('adminAttendanceDeleteSelected');
			const checkedCount = document.querySelectorAll('.attendance-row-checkbox:checked').length;
			
			if (deleteButton) {
				deleteButton.style.display = checkedCount > 0 ? 'inline-block' : 'none';
			}
		};

		// Handle bulk delete
		const deleteSelectedButton = document.getElementById('adminAttendanceDeleteSelected');
		if (deleteSelectedButton) {
			deleteSelectedButton.addEventListener('click', async () => {
				const checkboxes = document.querySelectorAll('.attendance-row-checkbox:checked');
				const selectedIds = Array.from(checkboxes).map(cb => cb.getAttribute('data-attendance-id'));

				if (selectedIds.length === 0) {
					showStatus('Please select at least one attendance record to delete.');
					return;
				}

				if (!confirm(`Are you sure you want to delete ${selectedIds.length} attendance record(s)? This action cannot be undone.`)) {
					return;
				}

				deleteSelectedButton.disabled = true;
				deleteSelectedButton.textContent = 'Deleting...';

				try {
					// Delete all selected records
					const deletePromises = selectedIds.map(id => 
						api(`/api/admin/attendance/${id}`, { method: 'DELETE' })
					);

					const results = await Promise.allSettled(deletePromises);
					const successful = results.filter(r => r.status === 'fulfilled' && r.value.ok).length;
					const failed = results.length - successful;

					if (successful > 0) {
						showStatus(`Successfully deleted ${successful} attendance record(s).${failed > 0 ? ` ${failed} failed.` : ''}`);
						await renderAttendanceTable();
						await loadNextAttendanceId();
					} else {
						showStatus('Failed to delete attendance records. Please try again.');
					}
				} catch (error) {
					console.error('Error deleting attendance records:', error);
					showStatus('Error deleting attendance records. Please try again.');
				} finally {
					deleteSelectedButton.disabled = false;
					deleteSelectedButton.textContent = '🗑️ Delete Selected';
				}
			});
		}

		// Initial load
		loadNextAttendanceId();
		loadActiveEmployees();
		renderAttendanceTable();
	};

	const initAdminLeave = () => {
		const listButton = document.getElementById('adminLeaveList');
		const form = document.getElementById('adminLeaveForm');
		const resultBox = document.getElementById('adminLeaveResult');
		const searchInput = document.getElementById('leaveSearchInput');
		const viewBalanceButton = document.getElementById('viewEmployeeBalance');
		const employeeBalanceSelect = document.getElementById('employeeBalanceSelect');
		const employeeBalanceResult = document.getElementById('employeeBalanceResult');
		
		let allLeaveRequests = [];

		// Modal functions
		const openProcessModal = () => {
			const modal = document.getElementById('processLeaveModal');
			if (modal) {
				modal.classList.add('show');
				document.body.style.overflow = 'hidden';
			}
		};

		const closeProcessModal = () => {
			const modal = document.getElementById('processLeaveModal');
			if (modal) {
				modal.classList.remove('show');
				document.body.style.overflow = '';
			}
		};

		// Close modal handlers
		const closeProcessBtn = document.getElementById('closeProcessLeaveModal');
		if (closeProcessBtn) {
			closeProcessBtn.addEventListener('click', closeProcessModal);
		}

		// Close modal when clicking outside
		document.addEventListener('click', (e) => {
			const processModal = document.getElementById('processLeaveModal');
			if (e.target === processModal) {
				closeProcessModal();
			}
		});
		
		// Initialize leave settings

		// Load and display admin's Employee ID and Full Name
		const loadAdminInfo = async () => {
			if (session.userId) {
				const decidedByInput = document.getElementById('decidedByHidden');
				const decidedByDisplay = document.getElementById('decidedByDisplay');
				
				// Always set the hidden field immediately
				if (decidedByInput) {
					decidedByInput.value = session.userId;
				}
				
				// Fetch admin's profile to get Employee ID and Full Name
				const res = await api(`/api/employee/profile/${session.userId}`);
				if (res.ok && res.payload && decidedByDisplay) {
					const employeeId = res.payload.employeeId || 'N/A';
					const fullName = res.payload.fullName || 'Unknown';
					decidedByDisplay.value = `${employeeId} - ${fullName}`;
				} else if (decidedByDisplay) {
					// Fallback if profile fetch fails
					decidedByDisplay.value = 'Admin User';
				}
			}
		};
		
		if (form) {
			loadAdminInfo();
		}

		// View Employee Leave Balance functionality
		const viewEmployeeLeaveBalance = async () => {
			if (!employeeBalanceSelect || !employeeBalanceResult) return;

			const selectedEmployeeId = employeeBalanceSelect.value;
			if (!selectedEmployeeId) {
				showStatus('Please select an employee');
				return;
			}

			// Fetch leave balance using the new search endpoint
			const balanceRes = await api(`/api/admin/leave-balance/search/${encodeURIComponent(selectedEmployeeId)}`);
			if (balanceRes.ok && balanceRes.payload) {
				const balances = balanceRes.payload.balances || [];
				const displayEmployeeId = balanceRes.payload.employeeId || 'N/A';

				// Fetch employee info to get full name
				let fullName = 'N/A';
				if (displayEmployeeId && displayEmployeeId !== 'N/A') {
					const userRes = await api(`/api/admin/users/by-employee-id/${displayEmployeeId}`);
					if (userRes.ok && userRes.payload) {
						fullName = userRes.payload.fullName || 'N/A';
					}
				}

				if (balances.length === 0) {
					employeeBalanceResult.innerHTML = `
						<div class="empty-state-modern">
							No leave balances found for Employee ID: ${displayEmployeeId}
						</div>
					`;
					return;
				}

				const getStatusClass = (balance) => {
					if (balance.isUnlimited) return 'status-pill-modern';
					if (balance.remainingDays <= 0) return 'status-pill-modern exhausted';
					if (balance.remainingDays < (balance.totalDays * 0.2)) return 'status-pill-modern low-balance';
					return 'status-pill-modern available';
				};

				const getStatusText = (balance) => {
					if (balance.isUnlimited) return '—';
					if (balance.remainingDays <= 0) return 'Exhausted';
					if (balance.remainingDays < (balance.totalDays * 0.2)) return 'Low Balance';
					return 'Available';
				};

				const table = `
					<div style="background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px; padding: 20px; margin-bottom: 24px; border: 1px solid var(--border-color);">
						<div style="display: flex; align-items: center; gap: 16px;">
							<div style="width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, #2f4ceb 0%, #1e3dd4 100%); color: white; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 1.1rem; flex-shrink: 0;">
								${(fullName || '?').split(' ').map(n => n[0]?.toUpperCase() || '').slice(0, 2).join('') || '?'}
							</div>
							<div style="flex: 1;">
								<div style="font-size: 1.1rem; font-weight: 600; color: var(--text-primary); margin-bottom: 4px;">${fullName || 'N/A'}</div>
								<div style="font-size: 0.9rem; color: var(--text-muted);">Employee ID: <span style="color: #2f4ceb; font-weight: 500;">${displayEmployeeId}</span></div>
							</div>
						</div>
					</div>
					<table class="table-modern">
						<thead>
							<tr>
								<th>Leave Type</th>
								<th style="text-align: center;">Total Days</th>
								<th style="text-align: center;">Used Days</th>
								<th style="text-align: center;">Remaining Days</th>
								<th style="text-align: center;">Status</th>
							</tr>
						</thead>
						<tbody>
							${balances.map(balance => {
								const isUnlimited = balance.isUnlimited;
								const totalDisplay = isUnlimited ? 'Unlimited' : balance.totalDays;
								const remainingDisplay = isUnlimited ? 'Unlimited' : balance.remainingDays;
								return `
									<tr>
										<td style="font-weight: 500;">${balance.leaveType || 'N/A'}</td>
										<td style="text-align: center; font-weight: 500;">${totalDisplay}</td>
										<td style="text-align: center;">${balance.usedDays || 0}</td>
										<td style="text-align: center; font-weight: 600; color: ${isUnlimited ? 'var(--text-primary)' : (balance.remainingDays <= 0 ? '#e74c3c' : (balance.remainingDays < (balance.totalDays * 0.2) ? '#f39c12' : '#27ae60'))};">${remainingDisplay}</td>
										<td style="text-align: center;">
											<span class="${getStatusClass(balance)}">${getStatusText(balance)}</span>
										</td>
									</tr>
								`;
							}).join('')}
						</tbody>
					</table>
				`;
				employeeBalanceResult.innerHTML = table;
			} else {
				const errorMsg = balanceRes.error || balanceRes.message || 'Unknown error';
				employeeBalanceResult.innerHTML = `
					<div class="empty-state-modern" style="color: #e74c3c;">
						Failed to load leave balance: ${errorMsg}
					</div>
				`;
			}
		};

		// Attach event listeners
		if (viewBalanceButton) {
			viewBalanceButton.addEventListener('click', viewEmployeeLeaveBalance);
		}

		if (employeeBalanceSelect) {
			employeeBalanceSelect.addEventListener('change', () => {
				const selectedEmployeeId = employeeBalanceSelect.value;
				if (selectedEmployeeId) {
					viewEmployeeLeaveBalance();
				} else {
					employeeBalanceResult.innerHTML = '<p style="text-align: center; color: var(--text-muted); padding: 2rem;">Select an employee to view leave balance.</p>';
				}
			});
		}

		const loadEmployeesForLeaveBalance = async () => {
			if (!employeeBalanceSelect) return;
			const res = await api('/api/admin/users');
			if (res.ok && Array.isArray(res.payload)) {
				const employees = res.payload.filter(user => user.employeeId && user.role === 'EMPLOYEE');
				employeeBalanceSelect.innerHTML = '<option value="">Select an employee (Active or Inactive)</option>';
				employees.forEach(user => {
					const option = document.createElement('option');
					const statusLabel = user.active ? 'Active' : 'Inactive';
					option.value = user.employeeId;
					option.textContent = `${user.employeeId} - ${user.fullName || user.username || 'Unnamed'} (${statusLabel})`;
					employeeBalanceSelect.appendChild(option);
				});
			}
		};
		loadEmployeesForLeaveBalance();

		const handleDeleteLeaveRequest = async (leaveId) => {
			if (!leaveId) return;
			if (!confirm(`Delete leave request ${leaveId}?`)) return;
			const res = await api(`/api/admin/leave/${leaveId}`, {
				method: 'DELETE'
			});
			if (res.ok) {
				showStatus('Leave request deleted');
				await fetchLeaveRequests();
				if (employeeBalanceSelect?.value) {
					await viewEmployeeLeaveBalance();
				}
			} else {
				const errorMsg = res.error || res.message || res.payload?.message || 'Failed to delete leave request';
				showStatus(errorMsg);
			}
		};

		const renderLeaveTable = (requests) => {
			if (!resultBox) return;
			
			if (!Array.isArray(requests) || requests.length === 0) {
				resultBox.innerHTML = '<div class="empty-state-modern">No leave requests found.</div>';
				return;
			}
			
			const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
			const filtered = requests.filter(req => {
				const leaveId = (req.leaveId || '').toString().toLowerCase();
				const employeeId = (req.employeeId || '').toString().toLowerCase();
				const employeeName = (req.employeeName || '').toString().toLowerCase();
				const status = (req.status || '').toString().toLowerCase();
				return leaveId.includes(searchTerm) ||
					employeeId.includes(searchTerm) ||
					employeeName.includes(searchTerm) ||
					status.includes(searchTerm);
			});
			
			if (filtered.length === 0) {
				resultBox.innerHTML = '<div class="empty-state-modern">No leave requests match your search.</div>';
				return;
			}

			const getStatusClass = (status) => {
				if (!status) return '';
				const statusLower = status.toLowerCase();
				return `status-pill-modern ${statusLower}`;
			};

			const formatStatus = (status) => {
				if (!status) return 'N/A';
				return status.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase());
			};

			const table = `
				<table class="table-modern">
					<thead>
						<tr>
							<th>
								<input type="checkbox" id="adminLeaveSelectAll" title="Select All" style="width: 18px; height: 18px; cursor: pointer; accent-color: #2f4ceb;">
							</th>
							<th>Employee ID</th>
							<th>Employee Name</th>
							<th>Leave Type</th>
							<th>Start Date</th>
							<th>End Date</th>
							<th>Reason (Notes)</th>
							<th>Supporting Document</th>
							<th>Status</th>
							<th>Admin Remarks</th>
							<th>Actions</th>
						</tr>
					</thead>
					<tbody>
						${filtered.map(req => {
							const documentLink = req.supportingDocumentFilename 
								? `<a href="/api/admin/leave-documents/${req.supportingDocumentFilename}" target="_blank" style="color: #2f4ceb; text-decoration: none; font-weight: 500;">📄 View Document</a>`
								: '<span style="color: var(--text-muted);">No document</span>';
							return `
								<tr>
									<td>
										<input type="checkbox" class="leave-row-checkbox" data-leave-id="${req.leaveId}" style="width: 18px; height: 18px; cursor: pointer; accent-color: #2f4ceb;">
									</td>
									<td>${req.employeeId || 'N/A'}</td>
									<td>${req.employeeName || 'N/A'}</td>
									<td>${req.leaveType || 'N/A'}</td>
									<td>${req.startDate ? new Date(req.startDate).toLocaleDateString() : 'N/A'}</td>
									<td>${req.endDate ? new Date(req.endDate).toLocaleDateString() : 'N/A'}</td>
									<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis;" title="${req.reason || ''}">${req.reason || '-'}</td>
									<td style="text-align: center;">${documentLink}</td>
									<td>
										<span class="${getStatusClass(req.status)}">${formatStatus(req.status)}</span>
									</td>
									<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis;" title="${req.managerComment || 'N/A'}">${req.managerComment || 'N/A'}</td>
									<td>
										<button class="action-btn-modern" data-action="decide-leave" data-leave-id="${req.leaveId}">Process</button>
										<button class="action-btn-modern danger" data-action="delete-leave" data-leave-id="${req.leaveId}">Delete</button>
									</td>
								</tr>
							`;
						}).join('')}
					</tbody>
				</table>
			`;
			resultBox.innerHTML = table;

			resultBox.querySelectorAll('[data-action="decide-leave"]').forEach(btn => {
				btn.addEventListener('click', () => {
					const leaveId = btn.getAttribute('data-leave-id');
					fillLeaveDecisionForm(leaveId);
					openProcessModal();
				});
			});

			resultBox.querySelectorAll('[data-action="delete-leave"]').forEach(btn => {
				btn.addEventListener('click', async () => {
					const leaveId = btn.getAttribute('data-leave-id');
					await handleDeleteLeaveRequest(leaveId);
				});
			});

			// Handle select all checkbox
			const selectAllCheckbox = document.getElementById('adminLeaveSelectAll');
			if (selectAllCheckbox) {
				selectAllCheckbox.addEventListener('change', (e) => {
					const checkboxes = resultBox.querySelectorAll('.leave-row-checkbox');
					checkboxes.forEach(cb => {
						cb.checked = e.target.checked;
					});
					updateLeaveDeleteButton();
				});
			}

			// Handle individual checkboxes
			resultBox.querySelectorAll('.leave-row-checkbox').forEach(checkbox => {
				checkbox.addEventListener('change', () => {
					updateLeaveSelectAllState();
					updateLeaveDeleteButton();
				});
			});
		};

		// Update select all checkbox state
		const updateLeaveSelectAllState = () => {
			const selectAllCheckbox = document.getElementById('adminLeaveSelectAll');
			if (!selectAllCheckbox) return;
			const checkboxes = document.querySelectorAll('.leave-row-checkbox');
			const checkedCount = document.querySelectorAll('.leave-row-checkbox:checked').length;
			
			if (checkboxes.length === 0) {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = false;
			} else if (checkedCount === 0) {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = false;
			} else if (checkedCount === checkboxes.length) {
				selectAllCheckbox.checked = true;
				selectAllCheckbox.indeterminate = false;
			} else {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = true;
			}
		};

		// Update delete button visibility
		const updateLeaveDeleteButton = () => {
			const deleteButton = document.getElementById('adminLeaveDeleteSelected');
			const checkedCount = document.querySelectorAll('.leave-row-checkbox:checked').length;
			
			if (deleteButton) {
				deleteButton.style.display = checkedCount > 0 ? 'inline-block' : 'none';
			}
		};

		const fetchLeaveRequests = async () => {
			try {
				const res = await api('/api/admin/leave');
				if (res.ok && res.payload) {
					allLeaveRequests = Array.isArray(res.payload) ? res.payload : [];
					renderLeaveTable(allLeaveRequests);
				} else {
					showStatus('Failed to fetch leave requests: ' + (res.message || 'Unknown error'));
					// Show empty state if no data
					if (resultBox) {
						resultBox.innerHTML = '<div class="empty-state-modern">No leave requests found.</div>';
					}
				}
			} catch (error) {
				console.error('Error fetching leave requests:', error);
				showStatus('Error loading leave requests');
				if (resultBox) {
					resultBox.innerHTML = '<p style="text-align: center; color: #c33;">Error loading leave requests. Please refresh the page.</p>';
				}
			}
		};

		// Auto-load on page load (button was removed, so always auto-load)
		fetchLeaveRequests();

		if (searchInput) {
			searchInput.addEventListener('input', () => {
				renderLeaveTable(allLeaveRequests);
			});
		}

		if (form) {
			form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const data = Object.fromEntries(new FormData(form));
				const { leaveId, ...body } = data;
				
				// Ensure decidedBy is set
				if (!body.decidedBy || body.decidedBy.trim() === '') {
					body.decidedBy = session.userId;
				}
				
				if (!leaveId || !body.status || !body.managerComment || !body.decidedBy) {
					showStatus('Please fill in all required fields');
					console.error('Missing fields:', { leaveId, status: body.status, managerComment: body.managerComment, decidedBy: body.decidedBy });
					return;
				}

				const res = await api(`/api/admin/leave/${leaveId}/decision`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(body)
				});
				
				if (res.ok) {
					showStatus('Leave decision submitted successfully');
					form.reset();
					loadAdminInfo(); // Re-populate admin info
					fetchLeaveRequests(); // Refresh the table
					closeProcessModal();
				} else {
					showStatus('Failed to submit decision: ' + (res.message || res.payload?.message || 'Unknown error'));
					console.error('API Error:', res);
				}
			});
		}
	};

	const fillLeaveDecisionForm = (leaveId) => {
		const form = document.getElementById('adminLeaveForm');
		if (form) {
			const leaveIdInput = form.querySelector('[name="leaveId"]') || document.getElementById('processLeaveId');
			if (leaveIdInput) {
				leaveIdInput.value = leaveId;
			}
		}
	};

	const initAdminOvertime = () => {
		const listButton = document.getElementById('adminOvertimeList');
		const statusFilter = document.getElementById('statusFilter');
		const tableContainer = document.getElementById('overtimeTableContainer');
		const form = document.getElementById('adminOvertimeForm');
		const processModal = document.getElementById('processOvertimeModal');
		const closeModalBtn = document.getElementById('closeProcessOvertimeModal');

		let allOvertimeRequests = [];

		const openProcessModal = () => {
			if (processModal) {
				processModal.classList.add('show');
				document.body.style.overflow = 'hidden';
			}
		};

		const closeProcessModal = () => {
			if (processModal) {
				processModal.classList.remove('show');
				document.body.style.overflow = '';
				if (form) {
					form.reset();
				}
			}
		};

		if (closeModalBtn) {
			closeModalBtn.addEventListener('click', closeProcessModal);
		}
		document.addEventListener('click', (e) => {
			if (e.target === processModal) {
				closeProcessModal();
			}
		});

		const renderOvertimeTable = (requests) => {
			if (!tableContainer) return;
			
			if (!Array.isArray(requests) || requests.length === 0) {
				tableContainer.innerHTML = '<div class="empty-state-modern">No overtime requests found.</div>';
				return;
			}

			const statusFilterValue = statusFilter ? statusFilter.value : '';
			const filtered = requests.filter(req => {
				if (!statusFilterValue) return true;
				return (req.status || '').toString().toUpperCase() === statusFilterValue.toUpperCase();
			});

			if (filtered.length === 0) {
				tableContainer.innerHTML = '<div class="empty-state-modern">No overtime requests match the selected filter.</div>';
				return;
			}

			const getStatusClass = (status) => {
				if (!status) return 'status-badge';
				const statusLower = status.toLowerCase();
				return `status-badge ${statusLower}`;
			};

			const table = `
				<table class="table-modern">
					<thead>
						<tr>
							<th>Overtime ID</th>
							<th>Employee ID</th>
							<th>Employee Name</th>
							<th>Work Date</th>
							<th>Start Time</th>
							<th>End Time</th>
							<th>Hours</th>
							<th>Reason</th>
							<th>Status</th>
							<th>Admin Remarks</th>
							<th>Actions</th>
						</tr>
					</thead>
					<tbody>
						${filtered.map(req => {
							return `
								<tr>
									<td>${req.overtimeId || 'N/A'}</td>
									<td>${req.employeeId || 'N/A'}</td>
									<td>${req.employeeName || 'N/A'}</td>
									<td>${req.workDate || 'N/A'}</td>
									<td>${req.startTime || 'N/A'}</td>
									<td>${req.endTime || 'N/A'}</td>
									<td>${req.hours ? req.hours.toFixed(2) : 'N/A'}</td>
									<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis;" title="${req.reason || ''}">${req.reason || '-'}</td>
									<td>
										<span class="${getStatusClass(req.status)}">${req.status || 'N/A'}</span>
									</td>
									<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis;" title="${req.managerComment || 'N/A'}">${req.managerComment || 'N/A'}</td>
									<td>
										<button class="action-btn-modern" data-action="decide-overtime" data-overtime-id="${req.overtimeId}">Process</button>
									</td>
								</tr>
							`;
						}).join('')}
					</tbody>
				</table>
			`;
			tableContainer.innerHTML = table;

			tableContainer.querySelectorAll('[data-action="decide-overtime"]').forEach(btn => {
				btn.addEventListener('click', () => {
					const overtimeId = btn.getAttribute('data-overtime-id');
					fillOvertimeDecisionForm(overtimeId);
					openProcessModal();
				});
			});
		};

		const fetchOvertimeRequests = async () => {
			try {
				const res = await api('/api/admin/overtime');
				if (res.ok && res.payload) {
					allOvertimeRequests = Array.isArray(res.payload) ? res.payload : [];
					renderOvertimeTable(allOvertimeRequests);
				} else {
					showStatus('Failed to fetch overtime requests: ' + (res.message || 'Unknown error'));
					if (tableContainer) {
						tableContainer.innerHTML = '<div class="empty-state-modern">No overtime requests found.</div>';
					}
				}
			} catch (error) {
				console.error('Error fetching overtime requests:', error);
				showStatus('Error loading overtime requests');
				if (tableContainer) {
					tableContainer.innerHTML = '<p style="text-align: center; color: #c33;">Error loading overtime requests. Please refresh the page.</p>';
				}
			}
		};

		if (listButton) {
			listButton.addEventListener('click', fetchOvertimeRequests);
		}

		if (statusFilter) {
			statusFilter.addEventListener('change', () => {
				renderOvertimeTable(allOvertimeRequests);
			});
		}

		if (form) {
			form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const data = Object.fromEntries(new FormData(form));
				const { overtimeId, ...body } = data;
				
				if (!body.decidedBy || body.decidedBy.trim() === '') {
					body.decidedBy = session.userId;
				}
				
				if (!overtimeId || !body.status || !body.managerComment || !body.decidedBy) {
					showStatus('Please fill in all required fields');
					return;
				}

				const res = await api(`/api/admin/overtime/${overtimeId}/decision`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(body)
				});
				
				if (res.ok) {
					showStatus('Overtime decision submitted successfully');
					form.reset();
					fetchOvertimeRequests();
					closeProcessModal();
				} else {
					showStatus('Failed to submit decision: ' + (res.message || res.payload?.message || 'Unknown error'));
				}
			});
		}

		// Auto-load on page load
		fetchOvertimeRequests();
	};

	const fillOvertimeDecisionForm = (overtimeId) => {
		const form = document.getElementById('adminOvertimeForm');
		if (form) {
			const overtimeIdInput = form.querySelector('[name="overtimeId"]') || document.getElementById('processOvertimeId');
			if (overtimeIdInput) {
				overtimeIdInput.value = overtimeId;
			}
		}
	};

	const initLeaveSettings = () => {
		const modal = document.getElementById('leaveSettingsModal');
		const openButton = document.getElementById('leaveSettingsButton');
		const cancelButton = document.getElementById('cancelLeaveSettings');
		const saveButton = document.getElementById('saveLeaveSettings');
		const addHolidayButton = document.getElementById('addHolidayButton');

		let currentSettings = null;
		let currentLeaveTypes = [];
		let currentHolidays = [];

		const loadSettings = async () => {
			const res = await api('/api/admin/leave-settings');
			if (res.ok && res.payload) {
				currentSettings = res.payload;
				currentLeaveTypes = currentSettings.availableLeaveTypes || [];
				currentHolidays = currentSettings.publicHolidays || [];
				renderLeaveTypes();
				renderHolidays();
			}
		};

		const renderLeaveTypes = () => {
			const container = document.getElementById('leaveTypesList');
			if (!container) return;

			if (currentLeaveTypes.length === 0) {
				container.innerHTML = '<div class="empty-state-modern">No leave types added yet.</div>';
				return;
			}

			container.innerHTML = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>Leave Type</th>
								<th style="width: 150px;">Days</th>
								<th style="text-align: center; width: 200px;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${currentLeaveTypes.map((type, index) => {
								const displayDays = type.daysAllowed === -1 ? 'Unlimited' : type.daysAllowed;
								return `
								<tr id="leaveTypeRow${index}">
									<td style="font-weight: 500;" id="leaveTypeName${index}">${type.name}</td>
									<td id="leaveTypeDays${index}">${displayDays}</td>
									<td style="text-align: center;">
										<div style="display: flex; gap: 8px; justify-content: center;">
											<button onclick="window.DashboardApp.editLeaveType(${index})" class="action-btn-modern">
												Edit
											</button>
											<button onclick="window.DashboardApp.removeLeaveType(${index})" class="action-btn-modern danger">
												Delete
											</button>
										</div>
									</td>
								</tr>
							`;}).join('')}
						</tbody>
					</table>
				</div>
			`;
		};

		const addLeaveType = () => {
			const nameInput = document.getElementById('newLeaveTypeName');
			const daysInput = document.getElementById('newLeaveTypeDays');

			if (!nameInput.value.trim()) {
				showStatus('Please enter a leave type name');
				return;
			}

			if (!daysInput.value.trim()) {
				showStatus('Please enter days (number or "unlimited")');
				return;
			}

			const newName = nameInput.value.trim();
			let daysAllowed = -1; // Default to unlimited

			const daysValue = daysInput.value.trim().toLowerCase();
			if (daysValue === 'unlimited') {
				daysAllowed = -1;
			} else {
				// Validate that it's a number
				const parsedDays = parseInt(daysValue, 10);
				if (isNaN(parsedDays) || parsedDays < 0 || !/^\d+$/.test(daysValue)) {
					showStatus('Days must be a positive number or "unlimited"');
					return;
				}
				daysAllowed = parsedDays;
			}

			// Check for duplicates
			if (currentLeaveTypes.some(lt => lt.name === newName)) {
				showStatus('This leave type already exists');
				return;
			}

			currentLeaveTypes.push({ name: newName, daysAllowed: daysAllowed });
			nameInput.value = '';
			daysInput.value = '';
			renderLeaveTypes();
		};

		const removeLeaveType = (index) => {
			currentLeaveTypes.splice(index, 1);
			renderLeaveTypes();
		};

		const editLeaveType = (index) => {
			const row = document.getElementById(`leaveTypeRow${index}`);
			const nameCell = document.getElementById(`leaveTypeName${index}`);
			const daysCell = document.getElementById(`leaveTypeDays${index}`);
			if (!row || !nameCell || !daysCell) return;

			const currentType = currentLeaveTypes[index];
			const displayDays = currentType.daysAllowed === -1 ? 'unlimited' : currentType.daysAllowed;

			// Replace cells with input fields
			nameCell.innerHTML = `<input type="text" id="editName${index}" class="input-modern" value="${currentType.name}" style="width: 100%; padding: 8px 12px;">`;
			daysCell.innerHTML = `<input type="text" id="editDays${index}" class="input-modern" value="${displayDays}" style="width: 100%; padding: 8px 12px;">`;

			// Replace action buttons
			const actionCell = row.cells[2];
			actionCell.innerHTML = `
				<div style="display: flex; gap: 8px; justify-content: center;">
					<button onclick="window.DashboardApp.saveLeaveType(${index})" class="action-btn-modern" style="color: #198754;">
						Save
					</button>
					<button onclick="window.DashboardApp.cancelEditLeaveType(${index})" class="action-btn-modern" style="color: #6c757d;">
						Cancel
					</button>
				</div>
			`;
		};

		const saveLeaveType = (index) => {
			const nameInput = document.getElementById(`editName${index}`);
			const daysInput = document.getElementById(`editDays${index}`);

			if (!nameInput || !daysInput) return;

			const newName = nameInput.value.trim();
			const daysValue = daysInput.value.trim().toLowerCase();

			if (!newName) {
				showStatus('Please enter a leave type name');
				return;
			}

			if (!daysValue) {
				showStatus('Please enter days (number or "unlimited")');
				return;
			}

			let daysAllowed = -1;
			if (daysValue === 'unlimited') {
				daysAllowed = -1;
			} else {
				const parsedDays = parseInt(daysValue, 10);
				if (isNaN(parsedDays) || parsedDays < 0 || !/^\d+$/.test(daysValue)) {
					showStatus('Days must be a positive number or "unlimited"');
					return;
				}
				daysAllowed = parsedDays;
			}

			// Check for duplicates (excluding current item)
			if (currentLeaveTypes.some((lt, i) => i !== index && lt.name === newName)) {
				showStatus('This leave type already exists');
				return;
			}

			// Update the leave type
			currentLeaveTypes[index] = { name: newName, daysAllowed: daysAllowed };
			renderLeaveTypes();
			showStatus('Leave type updated');
		};

		const cancelEditLeaveType = (index) => {
			renderLeaveTypes();
		};

		// Expose functions globally for initLeaveSettings
		window.DashboardApp.removeLeaveType = removeLeaveType;
		window.DashboardApp.editLeaveType = editLeaveType;
		window.DashboardApp.saveLeaveType = saveLeaveType;
		window.DashboardApp.cancelEditLeaveType = cancelEditLeaveType;

		const renderHolidays = () => {
			const container = document.getElementById('publicHolidaysList');
			if (!container) return;

			if (currentHolidays.length === 0) {
				container.innerHTML = '<div class="empty-state-modern">No public holidays added yet.</div>';
				return;
			}

			const sortedHolidays = [...currentHolidays].sort((a, b) => new Date(a.date) - new Date(b.date));

			container.innerHTML = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>Date</th>
								<th>Holiday Name</th>
								<th style="text-align: center; width: 120px;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${sortedHolidays.map((holiday, index) => `
								<tr>
									<td style="font-weight: 500;">${holiday.date}</td>
									<td>${holiday.name}</td>
									<td style="text-align: center;">
										<button onclick="window.DashboardApp.removeHoliday(${index})" class="action-btn-modern danger">
											Delete
										</button>
									</td>
								</tr>
							`).join('')}
						</tbody>
					</table>
				</div>
			`;
		};

		const addHoliday = () => {
			const dateInput = document.getElementById('newHolidayDate');
			const nameInput = document.getElementById('newHolidayName');

			if (!dateInput.value || !nameInput.value.trim()) {
				showStatus('Please enter both date and holiday name');
				return;
			}

			currentHolidays.push({
				date: dateInput.value,
				name: nameInput.value.trim()
			});

			dateInput.value = '';
			nameInput.value = '';
			renderHolidays();
		};

		const removeHoliday = (index) => {
			currentHolidays.splice(index, 1);
			renderHolidays();
		};

		const saveSettings = async () => {
			if (currentLeaveTypes.length === 0) {
				showStatus('Please add at least one leave type');
				return;
			}

			const payload = {
				availableLeaveTypes: currentLeaveTypes,
				publicHolidays: currentHolidays
			};

			const res = await api('/api/admin/leave-settings', {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload)
			});

			if (res.ok) {
				showStatus('Leave settings saved successfully');
				modal.style.display = 'none';
			} else {
				showStatus('Failed to save settings: ' + (res.message || 'Unknown error'));
			}
		};

		if (openButton) {
			openButton.addEventListener('click', () => {
				loadSettings();
				modal.style.display = 'block';
			});
		}

		if (cancelButton) {
			cancelButton.addEventListener('click', () => {
				modal.style.display = 'none';
			});
		}

		if (saveButton) {
			saveButton.addEventListener('click', saveSettings);
		}

		if (addHolidayButton) {
			addHolidayButton.addEventListener('click', addHoliday);
		}

		const addLeaveTypeButton = document.getElementById('addLeaveTypeButton');
		if (addLeaveTypeButton) {
			addLeaveTypeButton.addEventListener('click', addLeaveType);
		}

		// Expose functions globally
		return { removeHoliday, removeLeaveType, editLeaveType, saveLeaveType, cancelEditLeaveType };
	};

	const initAdminCompanySettings = () => {
		// Company Information Section
		const companyInfoForm = document.getElementById('companyInfoForm');
		const companyNameInput = document.getElementById('companyName');
		const companyAddressInput = document.getElementById('companyAddress');

		const loadCompanyInfo = async () => {
			const res = await api('/api/admin/settings');
			if (res.ok) {
				if (companyNameInput) {
					companyNameInput.value = res.payload.companyName || '';
				}
				if (companyAddressInput) {
					companyAddressInput.value = res.payload.companyAddress || '';
				}
			}
		};

		if (companyInfoForm) {
			companyInfoForm.addEventListener('submit', async (event) => {
				event.preventDefault();

				const companyName = companyNameInput?.value.trim() || '';
				const companyAddress = companyAddressInput?.value.trim() || '';

				// Validate company name
				if (companyName.length > 30) {
					showStatus('Company name cannot exceed 30 characters');
					return;
				}

				// Validate company address
				if (companyAddress.length > 250) {
					showStatus('Company address cannot exceed 250 characters');
					return;
				}

				// Get current settings to preserve other fields
				const currentRes = await api('/api/admin/settings');
				if (!currentRes.ok) {
					showStatus('Failed to load current settings');
					return;
				}

				const payload = {
					workStartTime: currentRes.payload.workStartTime || '09:00',
					workEndTime: currentRes.payload.workEndTime || '17:00',
					lateThresholdMinutes: currentRes.payload.lateThresholdMinutes || 0,
					workingDays: currentRes.payload.workingDays || ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
					companyName: companyName,
					companyAddress: companyAddress
				};

				const res = await api('/api/admin/settings', {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});

				if (res.ok) {
					showStatus('Company information saved successfully');
					// Reload company info to show updated values
					await loadCompanyInfo();
				} else {
					showStatus(res.message || 'Failed to save company information');
				}
			});
		}

		// Load company info on initialization
		loadCompanyInfo();

		// Department Management Section
		const departmentsList = document.getElementById('departmentsList');
		const newDepartmentName = document.getElementById('newDepartmentName');
		const newDepartmentDescription = document.getElementById('newDepartmentDescription');
		const addDepartmentButton = document.getElementById('addDepartmentButton');

		// Helper function to escape HTML
		const escapeHtml = (text) => {
			if (!text) return '';
			const div = document.createElement('div');
			div.textContent = text;
			return div.innerHTML;
		};

		// Store current departments for editing
		let currentDepartments = [];

		// Render departments list
		const renderDepartments = () => {
			if (!departmentsList) return;
			
			if (currentDepartments.length === 0) {
				departmentsList.innerHTML = '<div class="empty-state-modern">No departments found. Add your first department above.</div>';
				return;
			}

			departmentsList.innerHTML = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>Department Name</th>
								<th>Description</th>
								<th style="text-align: center; width: 200px;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${currentDepartments.map((dept, index) => `
								<tr id="deptRow${index}">
									<td style="font-weight: 500;" id="deptName${index}">${escapeHtml(dept.name)}</td>
									<td id="deptDesc${index}">${dept.description ? escapeHtml(dept.description) : '<span style="color: #999; font-style: italic;">No description</span>'}</td>
									<td style="text-align: center;">
										<div style="display: flex; gap: 8px; justify-content: center;">
											<button onclick="window.DashboardApp.editDepartment(${index})" class="action-btn-modern">
												Edit
											</button>
											<button onclick="window.DashboardApp.deleteDepartment(${index})" class="action-btn-modern danger">
												Delete
											</button>
										</div>
									</td>
								</tr>
							`).join('')}
						</tbody>
					</table>
				</div>
			`;
		};

		// Load and render departments
		const loadDepartmentsForManagement = async () => {
			if (!departmentsList) return;
			const res = await api('/api/admin/departments');
			if (res.ok && res.payload) {
				currentDepartments = res.payload;
				renderDepartments();
			}
		};

		// Add department
		if (addDepartmentButton) {
			addDepartmentButton.addEventListener('click', async () => {
				const name = newDepartmentName?.value.trim();
				if (!name) {
					showStatus('Department name is required');
					return;
				}
				const description = newDepartmentDescription?.value.trim() || '';
				const res = await api('/api/admin/departments', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ name, description })
				});
				if (res.ok) {
					showStatus('Department added successfully');
					newDepartmentName.value = '';
					newDepartmentDescription.value = '';
					await loadDepartmentsForManagement();
				} else {
					const errorMsg = res.error || res.message || 'Unknown error';
					showStatus('Failed to add department: ' + errorMsg);
				}
			});
		}

		// Edit department (inline editing)
		if (!window.DashboardApp) {
			window.DashboardApp = {};
		}
		window.DashboardApp.editDepartment = (index) => {
			const row = document.getElementById(`deptRow${index}`);
			const nameCell = document.getElementById(`deptName${index}`);
			const descCell = document.getElementById(`deptDesc${index}`);
			const actionsCell = document.getElementById(`deptActions${index}`);
			if (!row || !nameCell || !descCell || !actionsCell) return;

			const currentDept = currentDepartments[index];
			const currentName = currentDept.name || '';
			const currentDescription = currentDept.description || '';

			// Replace cells with input fields
			nameCell.innerHTML = `<input type="text" id="editDeptName${index}" class="input-modern" value="${escapeHtml(currentName)}" style="width: 100%; padding: 8px 12px;">`;
			descCell.innerHTML = `<input type="text" id="editDeptDesc${index}" class="input-modern" value="${escapeHtml(currentDescription)}" placeholder="Description (optional)" style="width: 100%; padding: 8px 12px;">`;

			// Replace action buttons
			actionsCell.innerHTML = `
				<div style="display: flex; gap: 8px; justify-content: center;">
					<button onclick="window.DashboardApp.saveDepartment(${index})" class="action-btn-modern" style="color: #198754;">
						Save
					</button>
					<button onclick="window.DashboardApp.cancelEditDepartment(${index})" class="action-btn-modern" style="color: #6c757d;">
						Cancel
					</button>
				</div>
			`;
		};

		// Save department
		window.DashboardApp.saveDepartment = async (index) => {
			const nameInput = document.getElementById(`editDeptName${index}`);
			const descInput = document.getElementById(`editDeptDesc${index}`);

			if (!nameInput || !descInput) return;

			const newName = nameInput.value.trim();
			const newDescription = descInput.value.trim();

			if (!newName) {
				showStatus('Department name is required');
				return;
			}

			const currentDept = currentDepartments[index];
			const deptId = currentDept.id;

			const res = await api(`/api/admin/departments/${deptId}`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ name: newName, description: newDescription })
			});

			if (res.ok) {
				showStatus('Department updated successfully');
				await loadDepartmentsForManagement();
			} else {
				const errorMsg = res.error || res.message || 'Unknown error';
				showStatus('Failed to update department: ' + errorMsg);
				renderDepartments(); // Re-render to cancel edit mode
			}
		};

		// Cancel edit department
		window.DashboardApp.cancelEditDepartment = (index) => {
			renderDepartments();
		};

		// Delete department
		window.DashboardApp.deleteDepartment = async (index) => {
			if (!confirm('Are you sure you want to delete this department?')) {
				return;
			}

			const currentDept = currentDepartments[index];
			const deptId = currentDept.id;

			const res = await api(`/api/admin/departments/${deptId}`, {
				method: 'DELETE'
			});

			if (res.ok) {
				showStatus('Department deleted successfully');
				await loadDepartmentsForManagement();
			} else {
				const errorMsg = res.error || res.message || 'Unknown error';
				showStatus('Failed to delete department: ' + errorMsg);
			}
		};

		// Load departments on initialization
		loadDepartmentsForManagement();

		// Company Working Hours Settings Section
		const settingsForm = document.getElementById('attendanceSettingsForm');
		const workingDayCheckboxes = Array.from(document.querySelectorAll('input[name="workingDayOption"]'));
		const defaultWorkingDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];

		const applyWorkingDaySelections = (selectedDays) => {
			const normalized = Array.isArray(selectedDays) && selectedDays.length
				? selectedDays.map(day => day.toUpperCase())
				: defaultWorkingDays;
			workingDayCheckboxes.forEach((checkbox) => {
				checkbox.checked = normalized.includes(checkbox.value.toUpperCase());
			});
		};

		const collectWorkingDaysFromForm = () => workingDayCheckboxes
			.filter(checkbox => checkbox.checked)
			.map(checkbox => checkbox.value);

		const loadAttendanceSettings = async () => {
			const res = await api('/api/admin/settings');
			if (res.ok) {
				document.getElementById('settingsWorkStartTime').value = res.payload.workStartTime || '09:00';
				document.getElementById('settingsWorkEndTime').value = res.payload.workEndTime || '17:00';
				document.getElementById('settingsLateThreshold').value = res.payload.lateThresholdMinutes || 0;
				applyWorkingDaySelections(res.payload.workingDays);
			} else {
				applyWorkingDaySelections(defaultWorkingDays);
			}
		};

		if (settingsForm) {
			settingsForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				
				const selectedWorkingDays = collectWorkingDaysFromForm();
				if (!selectedWorkingDays.length) {
					showStatus('Please select at least one operating day.');
					return;
				}

				// Get current settings to preserve company info
				const currentRes = await api('/api/admin/settings');
				if (!currentRes.ok) {
					showStatus('Failed to load current settings');
					return;
				}

				const payload = {
					workStartTime: document.getElementById('settingsWorkStartTime').value,
					workEndTime: document.getElementById('settingsWorkEndTime').value,
					lateThresholdMinutes: parseInt(document.getElementById('settingsLateThreshold').value) || 0,
					workingDays: selectedWorkingDays,
					companyName: currentRes.payload.companyName || '',
					companyAddress: currentRes.payload.companyAddress || ''
				};

				const res = await api('/api/admin/settings', {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});

				if (res.ok) {
					showStatus('Settings saved successfully');
				} else {
					showStatus(res.message || 'Failed to save settings');
				}
			});
		}

		// Load attendance settings on initialization
		loadAttendanceSettings();

		// Leave Management Settings Section
		const saveLeaveSettingsButton = document.getElementById('saveLeaveSettings');
		const addHolidayButton = document.getElementById('addHolidayButton');
		const addLeaveTypeButton = document.getElementById('addLeaveTypeButton');

		let currentSettings = null;
		let currentLeaveTypes = [];
		let currentHolidays = [];

		const loadLeaveSettings = async () => {
			const res = await api('/api/admin/leave-settings');
			if (res.ok && res.payload) {
				currentSettings = res.payload;
				currentLeaveTypes = currentSettings.availableLeaveTypes || [];
				currentHolidays = currentSettings.publicHolidays || [];
				renderLeaveTypes();
				renderHolidays();
			}
		};

		const renderLeaveTypes = () => {
			const container = document.getElementById('leaveTypesList');
			if (!container) return;

			if (currentLeaveTypes.length === 0) {
				container.innerHTML = '<div class="empty-state-modern">No leave types added yet.</div>';
				return;
			}

			container.innerHTML = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>Leave Type</th>
								<th style="width: 150px;">Days</th>
								<th style="text-align: center; width: 200px;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${currentLeaveTypes.map((type, index) => {
								const displayDays = type.daysAllowed === -1 ? 'Unlimited' : type.daysAllowed;
								return `
								<tr id="leaveTypeRow${index}">
									<td style="font-weight: 500;" id="leaveTypeName${index}">${type.name}</td>
									<td id="leaveTypeDays${index}">${displayDays}</td>
									<td style="text-align: center;">
										<div style="display: flex; gap: 8px; justify-content: center;">
											<button onclick="window.DashboardApp.editLeaveType(${index})" class="action-btn-modern">
												Edit
											</button>
											<button onclick="window.DashboardApp.removeLeaveType(${index})" class="action-btn-modern danger">
												Delete
											</button>
										</div>
									</td>
								</tr>
							`;}).join('')}
						</tbody>
					</table>
				</div>
			`;
		};

		const addLeaveType = () => {
			const nameInput = document.getElementById('newLeaveTypeName');
			const daysInput = document.getElementById('newLeaveTypeDays');

			if (!nameInput.value.trim()) {
				showStatus('Please enter a leave type name');
				return;
			}

			if (!daysInput.value.trim()) {
				showStatus('Please enter days (number or "unlimited")');
				return;
			}

			const newName = nameInput.value.trim();
			let daysAllowed = -1; // Default to unlimited

			const daysValue = daysInput.value.trim().toLowerCase();
			if (daysValue === 'unlimited') {
				daysAllowed = -1;
			} else {
				// Validate that it's a number
				const parsedDays = parseInt(daysValue, 10);
				if (isNaN(parsedDays) || parsedDays < 0 || !/^\d+$/.test(daysValue)) {
					showStatus('Days must be a positive number or "unlimited"');
					return;
				}
				daysAllowed = parsedDays;
			}

			// Check for duplicates
			if (currentLeaveTypes.some(lt => lt.name === newName)) {
				showStatus('This leave type already exists');
				return;
			}

			currentLeaveTypes.push({ name: newName, daysAllowed: daysAllowed });
			nameInput.value = '';
			daysInput.value = '';
			renderLeaveTypes();
		};

		const removeLeaveType = (index) => {
			currentLeaveTypes.splice(index, 1);
			renderLeaveTypes();
		};

		const editLeaveType = (index) => {
			const row = document.getElementById(`leaveTypeRow${index}`);
			const nameCell = document.getElementById(`leaveTypeName${index}`);
			const daysCell = document.getElementById(`leaveTypeDays${index}`);
			if (!row || !nameCell || !daysCell) return;

			const currentType = currentLeaveTypes[index];
			const displayDays = currentType.daysAllowed === -1 ? 'unlimited' : currentType.daysAllowed;

			// Replace cells with input fields
			nameCell.innerHTML = `<input type="text" id="editName${index}" class="input-modern" value="${currentType.name}" style="width: 100%; padding: 8px 12px;">`;
			daysCell.innerHTML = `<input type="text" id="editDays${index}" class="input-modern" value="${displayDays}" style="width: 100%; padding: 8px 12px;">`;

			// Replace action buttons
			const actionCell = row.cells[2];
			actionCell.innerHTML = `
				<div style="display: flex; gap: 8px; justify-content: center;">
					<button onclick="window.DashboardApp.saveLeaveType(${index})" class="action-btn-modern" style="color: #198754;">
						Save
					</button>
					<button onclick="window.DashboardApp.cancelEditLeaveType(${index})" class="action-btn-modern" style="color: #6c757d;">
						Cancel
					</button>
				</div>
			`;
		};

		const saveLeaveType = (index) => {
			const nameInput = document.getElementById(`editName${index}`);
			const daysInput = document.getElementById(`editDays${index}`);

			if (!nameInput || !daysInput) return;

			const newName = nameInput.value.trim();
			const daysValue = daysInput.value.trim().toLowerCase();

			if (!newName) {
				showStatus('Please enter a leave type name');
				return;
			}

			if (!daysValue) {
				showStatus('Please enter days (number or "unlimited")');
				return;
			}

			let daysAllowed = -1;
			if (daysValue === 'unlimited') {
				daysAllowed = -1;
			} else {
				const parsedDays = parseInt(daysValue, 10);
				if (isNaN(parsedDays) || parsedDays < 0 || !/^\d+$/.test(daysValue)) {
					showStatus('Days must be a positive number or "unlimited"');
					return;
				}
				daysAllowed = parsedDays;
			}

			// Check for duplicates (excluding current item)
			if (currentLeaveTypes.some((lt, i) => i !== index && lt.name === newName)) {
				showStatus('This leave type already exists');
				return;
			}

			// Update the leave type
			currentLeaveTypes[index] = { name: newName, daysAllowed: daysAllowed };
			renderLeaveTypes();
			showStatus('Leave type updated');
		};

		const cancelEditLeaveType = (index) => {
			renderLeaveTypes();
		};

		const renderHolidays = () => {
			const container = document.getElementById('publicHolidaysList');
			if (!container) return;

			if (currentHolidays.length === 0) {
				container.innerHTML = '<p style="text-align: center; color: #888; padding: 1rem;">No public holidays added yet.</p>';
				return;
			}

			const sortedHolidays = [...currentHolidays].sort((a, b) => new Date(a.date) - new Date(b.date));

			container.innerHTML = `
				<table style="width: 100%; border-collapse: collapse;">
					<thead>
						<tr style="background-color: #f5f5f5;">
							<th style="padding: 0.5rem; text-align: left; border-bottom: 2px solid #ddd;">Date</th>
							<th style="padding: 0.5rem; text-align: left; border-bottom: 2px solid #ddd;">Holiday Name</th>
							<th style="padding: 0.5rem; text-align: center; border-bottom: 2px solid #ddd; width: 100px;">Action</th>
						</tr>
					</thead>
					<tbody>
						${sortedHolidays.map((holiday, index) => {
							const originalIndex = currentHolidays.findIndex(h => h.date === holiday.date && h.name === holiday.name);
							return `
							<tr style="border-bottom: 1px solid #eee;">
								<td style="padding: 0.5rem;">${holiday.date}</td>
								<td style="padding: 0.5rem;">${holiday.name}</td>
								<td style="padding: 0.5rem; text-align: center;">
									<button onclick="window.DashboardApp.removeHoliday(${originalIndex})" 
											style="background: #dc3545; color: white; border: none; padding: 0.25rem 0.5rem; border-radius: 4px; cursor: pointer;">
										Delete
									</button>
								</td>
							</tr>
						`;}).join('')}
					</tbody>
				</table>
			`;
		};

		const addHoliday = () => {
			const dateInput = document.getElementById('newHolidayDate');
			const nameInput = document.getElementById('newHolidayName');

			if (!dateInput.value || !nameInput.value.trim()) {
				showStatus('Please enter both date and holiday name');
				return;
			}

			currentHolidays.push({
				date: dateInput.value,
				name: nameInput.value.trim()
			});

			dateInput.value = '';
			nameInput.value = '';
			renderHolidays();
		};

		const removeHoliday = (index) => {
			currentHolidays.splice(index, 1);
			renderHolidays();
		};

		const saveLeaveSettings = async () => {
			if (currentLeaveTypes.length === 0) {
				showStatus('Please add at least one leave type');
				return;
			}

			const payload = {
				availableLeaveTypes: currentLeaveTypes,
				publicHolidays: currentHolidays
			};

			const res = await api('/api/admin/leave-settings', {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(payload)
			});

			if (res.ok) {
				showStatus('Leave settings saved successfully');
			} else {
				showStatus('Failed to save settings: ' + (res.message || 'Unknown error'));
			}
		};

		if (saveLeaveSettingsButton) {
			saveLeaveSettingsButton.addEventListener('click', saveLeaveSettings);
		}

		if (addHolidayButton) {
			addHolidayButton.addEventListener('click', addHoliday);
		}

		if (addLeaveTypeButton) {
			addLeaveTypeButton.addEventListener('click', addLeaveType);
		}

		// Expose functions globally for initAdminCompanySettings
		window.DashboardApp.removeHoliday = removeHoliday;
		window.DashboardApp.removeLeaveType = removeLeaveType;
		window.DashboardApp.editLeaveType = editLeaveType;
		window.DashboardApp.saveLeaveType = saveLeaveType;
		window.DashboardApp.cancelEditLeaveType = cancelEditLeaveType;

		// Load leave settings on initialization
		loadLeaveSettings();
	};

	const initAdminPayroll = () => {
		const form = document.getElementById('adminPayrollForm');
		const listButton = document.getElementById('adminPayrollList');
		const resultBox = document.getElementById('adminPayrollResult');
		const employeeSelect = document.getElementById('employeeSelect');
		const salaryMonthInput = document.getElementById('salaryMonth');

		console.log('Initializing admin payroll...', {
			form: !!form,
			listButton: !!listButton,
			resultBox: !!resultBox,
			employeeSelect: !!employeeSelect,
			salaryMonthInput: !!salaryMonthInput
		});

		// Set default month to current month
		if (salaryMonthInput) {
			const now = new Date();
			const year = now.getFullYear();
			const month = String(now.getMonth() + 1).padStart(2, '0');
			salaryMonthInput.value = `${year}-${month}`;
		}

		// Load active employees
		const loadActiveEmployees = async () => {
			if (!employeeSelect) {
				console.error('Employee select element not found');
				return;
			}

			try {
				// Show loading state
				employeeSelect.innerHTML = '<option value="">Loading employees...</option>';
				employeeSelect.disabled = true;

				const res = await api('/api/admin/users');
				
				if (!res.ok) {
					throw new Error(res.message || 'Failed to fetch employees');
				}

				if (!Array.isArray(res.payload)) {
					throw new Error('Invalid response format');
				}

				// Filter active employees with EMPLOYEE role (case-insensitive)
				const activeEmployees = res.payload.filter(user => {
					const isActive = user.active === true || user.active === 'true';
					const isEmployee = (user.role && user.role.toUpperCase() === 'EMPLOYEE') || 
									   (user.role === 'EMPLOYEE');
					const hasEmployeeId = user.employeeId && user.employeeId.trim() !== '';
					return isActive && isEmployee && hasEmployeeId;
				});

				// Clear and populate dropdown
				employeeSelect.innerHTML = '<option value="">-- Select an Employee --</option>';
				
				if (activeEmployees.length === 0) {
					employeeSelect.innerHTML = '<option value="">No active employees found</option>';
					showStatus('No active employees found. Please ensure employees have an Employee ID assigned.');
				} else {
					// Sort employees by name for better UX
					activeEmployees.sort((a, b) => {
						const nameA = (a.fullName || a.username || '').toLowerCase();
						const nameB = (b.fullName || b.username || '').toLowerCase();
						return nameA.localeCompare(nameB);
					});

					activeEmployees.forEach(user => {
						const option = document.createElement('option');
						option.value = user.id; // Use userId for payroll calculation
						const displayName = user.fullName || user.username || 'Unknown';
						const employeeId = user.employeeId || 'N/A';
						option.textContent = `${displayName} (${employeeId})`;
						employeeSelect.appendChild(option);
					});
					console.log(`Loaded ${activeEmployees.length} active employees into dropdown`);
				}

				employeeSelect.disabled = false;
			} catch (error) {
				console.error('Error loading employees:', error);
				employeeSelect.innerHTML = '<option value="">Error loading employees</option>';
				employeeSelect.disabled = false;
				showStatus(`Failed to load employees: ${error.message}`);
			}
		};

		// Load employees when page loads
		loadActiveEmployees();

		if (form) {
			form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(form);
				const userId = formData.get('userId');
				const salaryMonth = formData.get('salaryMonth');

				if (!userId || !salaryMonth) {
					showStatus('Please select an employee and month.');
					return;
				}

				// Parse month to YearMonth format (YYYY-MM)
				const [year, month] = salaryMonth.split('-');

				showStatus('Calculating payroll...');

				try {
					const res = await api('/api/admin/payroll/calculate', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify({
							userId: userId,
							month: salaryMonth // Send as "YYYY-MM" string
						})
					});

					if (res.ok && res.payload) {
						showStatus('Payslip generated successfully!');
						// Small delay to show success message
						setTimeout(() => {
							window.location.href = `/payslip.html?userId=${userId}&year=${year}&month=${month}`;
						}, 500);
					} else {
						// Try to extract error message from response
						let errorMsg = 'Failed to generate payslip.';
						if (res.payload) {
							if (typeof res.payload === 'string') {
								errorMsg = res.payload;
							} else if (res.payload.message) {
								errorMsg = res.payload.message;
							} else if (res.payload.error) {
								errorMsg = res.payload.error;
							}
						} else if (res.rawText) {
							// Try to parse raw text as JSON
							try {
								const errorData = JSON.parse(res.rawText);
								errorMsg = errorData.message || errorData.error || errorMsg;
							} catch (e) {
								errorMsg = res.rawText || errorMsg;
							}
						}
						console.error('Payroll calculation failed:', {
							status: res.status,
							payload: res.payload,
							rawText: res.rawText
						});
						showStatus(errorMsg);
					}
				} catch (error) {
					console.error('Error generating payslip:', error);
					showStatus(`Error: ${error.message || 'Failed to generate payslip'}`);
				}
			});
		}

		// Load payroll records on page load
		const loadPayrollRecords = async () => {
			if (!resultBox) return;

				const res = await api('/api/admin/payroll');
			if (res.ok && Array.isArray(res.payload)) {
				if (res.payload.length === 0) {
					resultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 40px;">No payroll records found.</p>';
					return;
				}

				// Fetch user list to get employee names
				const userRes = await api('/api/admin/users');
				const userMap = new Map();
				if (userRes.ok && Array.isArray(userRes.payload)) {
					userRes.payload.forEach(user => {
						userMap.set(user.id, user);
					});
				}

				// Format date helper
				const formatDate = (dateStr) => {
					if (!dateStr) return 'N/A';
					try {
						const date = new Date(dateStr);
						return date.toLocaleDateString('en-MY', { year: 'numeric', month: 'short', day: 'numeric' });
					} catch {
						return dateStr;
					}
				};

				// Format currency helper
				const formatCurrency = (amount) => {
					if (amount == null) return 'RM 0.00';
					return 'RM ' + parseFloat(amount).toFixed(2);
				};

				// Extract month from period
				const getMonthFromPeriod = (periodStart, periodEnd) => {
					if (!periodStart) return 'N/A';
					try {
						const date = new Date(periodStart);
						return date.toLocaleDateString('en-MY', { year: 'numeric', month: 'long' });
					} catch {
						return 'N/A';
					}
				};

				// Sort by period end date (newest first)
				const sortedPayrolls = [...res.payload].sort((a, b) => {
					if (!a.periodEnd || !b.periodEnd) return 0;
					return new Date(b.periodEnd) - new Date(a.periodEnd);
				});

				let html = `
					<div style="overflow-x: auto;">
						<table style="width: 100%; border-collapse: separate; border-spacing: 0; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);">
							<thead>
								<tr style="background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);">
									<th style="padding: 14px 16px; text-align: center; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6; width: 50px;">
										<input type="checkbox" id="selectAllPayroll" style="width: 18px; height: 18px; cursor: pointer; accent-color: #2f4ceb;" title="Select All">
									</th>
									<th style="padding: 14px 16px; text-align: left; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6;">Employee</th>
									<th style="padding: 14px 16px; text-align: left; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6;">Period</th>
									<th style="padding: 14px 16px; text-align: right; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6;">Gross Pay</th>
									<th style="padding: 14px 16px; text-align: right; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6;">Deductions</th>
									<th style="padding: 14px 16px; text-align: right; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6;">Net Pay</th>
									<th style="padding: 14px 16px; text-align: center; font-weight: 600; font-size: 0.85rem; color: #495057; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #dee2e6; width: 120px;">Actions</th>
								</tr>
							</thead>
							<tbody>
				`;

				sortedPayrolls.forEach((payroll, index) => {
					const user = userMap.get(payroll.userId);
					const employeeName = user ? (user.fullName || user.username || 'Unknown') : 'Unknown';
					const employeeId = user ? (user.employeeId || 'N/A') : 'N/A';
					const periodMonth = getMonthFromPeriod(payroll.periodStart, payroll.periodEnd);
					
					// Calculate gross pay (baseSalary + bonus)
					const grossPay = (parseFloat(payroll.baseSalary || 0) + parseFloat(payroll.bonus || 0));
					
					// Extract year and month for payslip link
					let year = '';
					let month = '';
					if (payroll.periodStart) {
						try {
							const date = new Date(payroll.periodStart);
							year = date.getFullYear();
							month = String(date.getMonth() + 1).padStart(2, '0');
						} catch (e) {
							// Use current date as fallback
							const now = new Date();
							year = now.getFullYear();
							month = String(now.getMonth() + 1).padStart(2, '0');
						}
					}

					const rowStyle = index % 2 === 0 ? 'background-color: #ffffff;' : 'background-color: #f8f9fa;';
					
					html += `
						<tr style="${rowStyle} transition: background-color 0.2s ease;" onmouseover="this.style.backgroundColor='#e9ecef'" onmouseout="this.style.backgroundColor='${index % 2 === 0 ? '#ffffff' : '#f8f9fa'}'">
							<td style="padding: 14px 16px; text-align: center; border-bottom: 1px solid #f0f0f0;">
								<input type="checkbox" class="payroll-checkbox" value="${payroll.id}" 
									style="width: 18px; height: 18px; cursor: pointer; accent-color: #2f4ceb;"
									onchange="DashboardApp.updateDeleteButton()">
							</td>
							<td style="padding: 14px 16px; font-size: 0.9rem; color: #212529; border-bottom: 1px solid #f0f0f0;">
								<div style="display: flex; flex-direction: column; gap: 4px;">
									<span style="font-weight: 600; color: #212529;">${employeeName}</span>
									<span style="font-size: 0.85rem; color: #6c757d;">${employeeId}</span>
								</div>
							</td>
							<td style="padding: 14px 16px; font-size: 0.9rem; color: #212529; border-bottom: 1px solid #f0f0f0;">
								<div style="display: flex; flex-direction: column; gap: 4px;">
									<span style="font-weight: 500;">${periodMonth}</span>
									<span style="font-size: 0.85rem; color: #6c757d;">${formatDate(payroll.periodStart)} - ${formatDate(payroll.periodEnd)}</span>
								</div>
							</td>
							<td style="padding: 14px 16px; text-align: right; font-size: 0.9rem; color: #212529; border-bottom: 1px solid #f0f0f0; font-weight: 500;">
								${formatCurrency(grossPay)}
							</td>
							<td style="padding: 14px 16px; text-align: right; font-size: 0.9rem; color: #dc3545; border-bottom: 1px solid #f0f0f0;">
								${formatCurrency(payroll.deductions)}
							</td>
							<td style="padding: 14px 16px; text-align: right; font-size: 0.9rem; color: #28a745; border-bottom: 1px solid #f0f0f0; font-weight: 600;">
								${formatCurrency(payroll.netPay)}
							</td>
							<td style="padding: 14px 16px; text-align: center; border-bottom: 1px solid #f0f0f0;">
								<button onclick="window.location.href='/payslip.html?userId=${payroll.userId}&year=${year}&month=${month}'" 
									style="padding: 6px 12px; background: linear-gradient(135deg, #2f4ceb 0%, #1e3dd4 100%); color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 0.85rem; font-weight: 500; transition: all 0.2s ease; box-shadow: 0 2px 4px rgba(47, 76, 235, 0.2);"
									onmouseover="this.style.transform='translateY(-1px)'; this.style.boxShadow='0 4px 8px rgba(47, 76, 235, 0.3)'"
									onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 4px rgba(47, 76, 235, 0.2)'"
									title="View Payslip">
									View Payslip
								</button>
							</td>
						</tr>
					`;
				});

				html += `
							</tbody>
						</table>
					</div>
				`;

				resultBox.innerHTML = html;

				// Setup select all checkbox
				const selectAllCheckbox = document.getElementById('selectAllPayroll');
				if (selectAllCheckbox) {
					selectAllCheckbox.addEventListener('change', (e) => {
						const checkboxes = document.querySelectorAll('.payroll-checkbox');
						checkboxes.forEach(cb => {
							cb.checked = e.target.checked;
						});
						updateDeleteButton();
					});
				}

				// Setup individual checkboxes
				const checkboxes = document.querySelectorAll('.payroll-checkbox');
				checkboxes.forEach(cb => {
					cb.addEventListener('change', () => {
						updateSelectAllState();
						updateDeleteButton();
					});
				});

				updateDeleteButton();
			} else {
				resultBox.innerHTML = '<p style="text-align: center; color: #dc3545; padding: 20px;">Failed to load payroll records.</p>';
			}
		};

		// Update select all checkbox state
		const updateSelectAllState = () => {
			const selectAllCheckbox = document.getElementById('selectAllPayroll');
			if (!selectAllCheckbox) return;

			const checkboxes = document.querySelectorAll('.payroll-checkbox');
			const checkedCount = document.querySelectorAll('.payroll-checkbox:checked').length;
			
			if (checkboxes.length === 0) {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = false;
			} else if (checkedCount === 0) {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = false;
			} else if (checkedCount === checkboxes.length) {
				selectAllCheckbox.checked = true;
				selectAllCheckbox.indeterminate = false;
			} else {
				selectAllCheckbox.checked = false;
				selectAllCheckbox.indeterminate = true;
			}
		};

		// Update delete button state
		const updateDeleteButton = () => {
			const deleteButton = document.getElementById('deleteSelectedPayroll');
			const selectedCountSpan = document.getElementById('selectedCount');
			const checkboxes = document.querySelectorAll('.payroll-checkbox:checked');
			const selectedCount = checkboxes.length;

			if (deleteButton) {
				if (selectedCount > 0) {
					deleteButton.style.display = 'inline-block';
					deleteButton.disabled = false;
					if (selectedCountSpan) {
						selectedCountSpan.textContent = selectedCount;
					}
				} else {
					deleteButton.style.display = 'none';
					deleteButton.disabled = true;
					if (selectedCountSpan) {
						selectedCountSpan.textContent = '0';
					}
				}
			}
		};

		// Expose updateDeleteButton to global scope
		window.DashboardApp = window.DashboardApp || {};
		window.DashboardApp.updateDeleteButton = updateDeleteButton;

		// Load on page load
		loadPayrollRecords();

		// Keep the old listButton for backward compatibility
		if (listButton) {
			listButton.addEventListener('click', () => {
				if (resultBox) {
					resultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 20px;">Loading payroll records...</p>';
				}
				loadPayrollRecords();
			});
		}

		// Setup delete selected button
		const deleteSelectedButton = document.getElementById('deleteSelectedPayroll');
		if (deleteSelectedButton) {
			deleteSelectedButton.addEventListener('click', async () => {
				const checkboxes = document.querySelectorAll('.payroll-checkbox:checked');
				const selectedIds = Array.from(checkboxes).map(cb => cb.value);

				if (selectedIds.length === 0) {
					showStatus('Please select at least one payroll record to delete.');
					return;
				}

				const count = selectedIds.length;
				const confirmMessage = count === 1
					? 'Are you sure you want to delete this payroll record? This action cannot be undone.'
					: `Are you sure you want to delete ${count} payroll records? This action cannot be undone.`;

				if (!confirm(confirmMessage)) {
					return;
				}

				showStatus(`Deleting ${count} payroll record(s)...`);
				deleteSelectedButton.disabled = true;

				try {
					// Delete all selected records
					const deletePromises = selectedIds.map(id => 
						api(`/api/admin/payroll/${id}`, { method: 'DELETE' })
					);

					const results = await Promise.allSettled(deletePromises);
					
					let successCount = 0;
					let failCount = 0;

					results.forEach((result, index) => {
						if (result.status === 'fulfilled' && (result.value.ok || result.value.status === 204)) {
							successCount++;
						} else {
							failCount++;
							console.error(`Failed to delete payroll ${selectedIds[index]}:`, result);
						}
					});

					if (failCount === 0) {
						showStatus(`Successfully deleted ${successCount} payroll record(s)!`);
					} else {
						showStatus(`Deleted ${successCount} record(s), but ${failCount} failed.`);
					}

					// Reload the table
					setTimeout(() => {
						if (resultBox) {
							resultBox.innerHTML = '<p style="text-align: center; color: #888; padding: 20px;">Loading payroll records...</p>';
						}
						loadPayrollRecords();
					}, 500);
				} catch (error) {
					console.error('Error deleting payroll records:', error);
					showStatus(`Error: ${error.message || 'Failed to delete payroll records'}`);
					deleteSelectedButton.disabled = false;
				}
			});
		}
	};

		const initAdminAnnouncements = () => {
		const form = document.getElementById('adminAnnouncementForm');
		const tableBody = document.getElementById('adminAnnouncementTableBody');
		const emptyState = document.getElementById('adminAnnouncementEmptyState');
		
		// Load announcements on page load
		const loadAnnouncements = async () => {
			const res = await api('/api/admin/announcements');
			if (res.ok && res.payload) {
				renderAnnouncementsTable(res.payload);
			} else {
				if (tableBody) tableBody.innerHTML = '';
				if (emptyState) emptyState.style.display = 'block';
			}
		};

		// Render announcements table
		const renderAnnouncementsTable = (announcements) => {
			if (!tableBody) return;
			if (!announcements || announcements.length === 0) {
				tableBody.innerHTML = '';
				if (emptyState) emptyState.style.display = 'block';
				return;
			}
			if (emptyState) emptyState.style.display = 'none';
			
			const today = new Date();
			today.setHours(0, 0, 0, 0);
			
			tableBody.innerHTML = announcements.map(announcement => {
				// Handle createdAt - can be string (ISO) or Instant
				let createdDate = 'N/A';
				if (announcement.createdAt) {
					if (typeof announcement.createdAt === 'string') {
						createdDate = new Date(announcement.createdAt).toLocaleDateString();
					} else {
						createdDate = new Date(announcement.createdAt).toLocaleDateString();
					}
				}
				
				const expiresDate = announcement.expiresOn ? new Date(announcement.expiresOn).toLocaleDateString() : 'Never';
				const expiresDateObj = announcement.expiresOn ? new Date(announcement.expiresOn) : null;
				expiresDateObj?.setHours(0, 0, 0, 0);
				
				const isExpired = expiresDateObj && expiresDateObj < today;
				const isPinned = announcement.pinned === true;
				
				let statusBadge = '';
				if (isPinned) {
					statusBadge = '<span class="status-pill-modern pinned">Pinned</span>';
				} else if (isExpired) {
					statusBadge = '<span class="status-pill-modern expired">Expired</span>';
				} else {
					statusBadge = '<span class="status-pill-modern active">Active</span>';
				}
				
				const message = announcement.message || 'No message';
				const truncatedMessage = message.length > 50 ? message.substring(0, 50) + '...' : message;
				
				return `
					<tr>
						<td style="font-weight: 500;">${announcement.title || 'N/A'}</td>
						<td class="announcement-message" title="${message.replace(/"/g, '&quot;')}">${truncatedMessage}</td>
						<td>${announcement.audience || 'N/A'}</td>
						<td>${announcement.createdBy || 'N/A'}</td>
						<td>${createdDate}</td>
						<td>${expiresDate}</td>
						<td>${statusBadge}</td>
						<td style="text-align: center;">
							<button class="delete-announcement-btn action-btn-modern danger" data-id="${announcement.id}">Delete</button>
						</td>
					</tr>
				`;
			}).join('');
			
			// Add event listeners for delete buttons
			document.querySelectorAll('.delete-announcement-btn').forEach(btn => {
				btn.addEventListener('click', async () => {
					const id = btn.dataset.id;
					if (confirm('Are you sure you want to delete this announcement?')) {
						const res = await api(`/api/admin/announcements/${id}`, {
							method: 'DELETE'
						});
						if (res.ok) {
							showStatus('Announcement deleted successfully');
							loadAnnouncements();
						} else {
							showStatus('Failed to delete announcement');
						}
					}
				});
			});
		};

		if (form) {
			form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(form);
				formData.set('pinned', form.pinned.checked);
				const data = Object.fromEntries(formData.entries());
				const res = await api('/api/admin/announcements', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(data)
				});
				if (res.ok) {
					showStatus('Announcement published successfully');
					form.reset();
					loadAnnouncements();
				} else {
					showStatus('Failed to publish announcement');
				}
			});
		}
		
		// Load announcements on page load
		loadAnnouncements();
	};

	const initAdminBenefits = () => {
		const form = document.getElementById('benefitForm');
		const resultBox = document.getElementById('benefitResult');
		const searchInput = document.getElementById('benefitSearchInput');
		const searchButton = document.getElementById('benefitSearchButton');
		const submitButton = document.getElementById('benefitSubmitButton');
		const cancelButton = document.getElementById('benefitCancelButton');
		const benefitIdInput = document.getElementById('benefitId');
		const benefitAmountInput = document.getElementById('benefitAmountInput');

		let isEditMode = false;

		// Load next Benefit ID
		const loadNextBenefitId = async () => {
			const displayBenefitId = document.getElementById('displayBenefitId');
			if (!displayBenefitId) return;

			const res = await api('/api/admin/benefit-categories/next-benefit-id');
			if (res.ok && res.payload) {
				displayBenefitId.value = res.payload;
			} else {
				// Fallback to B001 if API call fails
				displayBenefitId.value = 'B001';
			}
		};

		// Load next Benefit ID on page load
		loadNextBenefitId();

		// Real-time validation for benefit amount input
		if (benefitAmountInput) {
			benefitAmountInput.addEventListener('input', (e) => {
				const value = e.target.value.trim();
				// Remove leading zeros (except for "0" or "0.xx")
				if (value.length > 1 && value.startsWith('0') && !value.startsWith('0.')) {
					e.target.value = value.replace(/^0+/, '');
				}
			});

			benefitAmountInput.addEventListener('blur', (e) => {
				const value = e.target.value.trim();
				if (value && parseFloat(value) <= 0) {
					e.target.setCustomValidity('Benefit amount must be greater than 0');
				} else {
					e.target.setCustomValidity('');
				}
			});
		}

		// Load and display benefit categories
		const loadBenefitCategories = async (searchTerm = '') => {
			if (!resultBox) return;

			const url = searchTerm 
				? `/api/admin/benefit-categories?search=${encodeURIComponent(searchTerm)}`
				: '/api/admin/benefit-categories';
			
			const res = await api(url);
			if (res.ok && res.payload) {
				renderBenefitTable(res.payload);
			} else {
				resultBox.innerHTML = '<p style="text-align: center; color: #888;">Failed to load benefit categories.</p>';
			}
		};

		// Render benefit categories table
		const renderBenefitTable = (categories) => {
			if (!resultBox) return;

			const emptyState = document.getElementById('benefitEmptyState');
			if (!categories || categories.length === 0) {
				resultBox.innerHTML = '';
				if (emptyState) {
					emptyState.style.display = 'block';
				}
				return;
			}

			if (emptyState) {
				emptyState.style.display = 'none';
			}

			const table = `
				<table class="table-modern">
					<thead>
						<tr>
							<th>Name</th>
							<th>Description</th>
							<th style="text-align: right;">Benefit Amount (RM)</th>
							<th style="text-align: center;">Status</th>
							<th style="text-align: center;">Actions</th>
						</tr>
					</thead>
					<tbody>
						${categories.map(category => {
							const amount = (category.benefitAmount != null && category.benefitAmount !== undefined) 
								? parseFloat(category.benefitAmount).toFixed(2) 
								: 'N/A';
							const isActive = category.active !== false;
							return `
							<tr>
								<td style="font-weight: 500;">${category.name || 'N/A'}</td>
								<td style="color: var(--text-muted);">${category.description || 'No description'}</td>
								<td style="text-align: right; font-weight: 500;">RM ${amount}</td>
								<td style="text-align: center;">
									<span class="status-pill-modern ${isActive ? 'approved' : 'cancelled'}">${isActive ? 'Active' : 'Inactive'}</span>
								</td>
								<td style="text-align: center;">
									<div style="display: flex; gap: 8px; justify-content: center; align-items: center; flex-wrap: wrap;">
										<button class="action-btn-modern" onclick="window.DashboardApp.editBenefitCategory('${category.id}')">Edit</button>
										<button class="action-btn-modern danger" onclick="window.DashboardApp.toggleBenefitCategoryStatus('${category.id}', ${isActive})">${isActive ? 'Deactivate' : 'Activate'}</button>
									</div>
								</td>
							</tr>
							`;
						}).join('')}
					</tbody>
				</table>
			`;
			resultBox.innerHTML = table;
		};

		// Edit benefit category
		window.DashboardApp = window.DashboardApp || {};
		window.DashboardApp.editBenefitCategory = async (id) => {
			if (!form) return;

			const res = await api(`/api/admin/benefit-categories/${id}`);
			if (res.ok && res.payload) {
				const category = res.payload;
				form.name.value = category.name || '';
				form.description.value = category.description || '';
				form.benefitAmount.value = category.benefitAmount != null ? category.benefitAmount : '';
				benefitIdInput.value = category.id;
				
				// Hide Benefit ID display in edit mode
				const displayBenefitId = document.getElementById('displayBenefitId');
				if (displayBenefitId) {
					displayBenefitId.value = category.benefitId || 'N/A';
				}
				
				isEditMode = true;
				submitButton.textContent = 'Update Benefit Category';
				cancelButton.style.display = 'inline-block';
				
				form.scrollIntoView({ behavior: 'smooth', block: 'center' });
			} else {
				showStatus('Failed to load benefit category');
			}
		};

		// Delete benefit category
		window.DashboardApp.toggleBenefitCategoryStatus = async (id, currentlyActive) => {
			const action = currentlyActive ? 'deactivate' : 'activate';
			if (!confirm(`Are you sure you want to ${action} this benefit category?`)) {
				return;
			}

			const res = await api(`/api/admin/benefit-categories/${id}/status`, {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ active: !currentlyActive })
			});

			if (res.ok) {
				showStatus(`Benefit category ${currentlyActive ? 'deactivated' : 'activated'} successfully`);
				loadBenefitCategories(searchInput?.value.trim() || '');
				if (typeof loadBenefitCategoriesForAssignment === 'function') {
					loadBenefitCategoriesForAssignment();
				}
			} else {
				const errorMsg = res.error || res.message || 'Unknown error';
				showStatus(`Failed to ${action} benefit category: ` + errorMsg);
			}
		};

		// Form submission
		if (form) {
			form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(form);
				const benefitAmountValue = formData.get('benefitAmount');
				
				// Validate benefit amount
				if (!benefitAmountValue || benefitAmountValue.trim() === '') {
					showStatus('Benefit amount is required');
					return;
				}

				// Check if starts with 0 but is not exactly "0" or "0.xx"
				const trimmedValue = benefitAmountValue.trim();
				if (trimmedValue.length > 1 && trimmedValue.startsWith('0') && !trimmedValue.startsWith('0.')) {
					showStatus('Benefit amount cannot start with 0');
					return;
				}

				const benefitAmount = parseFloat(trimmedValue);

				if (isNaN(benefitAmount)) {
					showStatus('Benefit amount must be a valid number');
					return;
				}

				if (benefitAmount <= 0) {
					showStatus('Benefit amount must be greater than 0');
					return;
				}

				const data = {
					name: formData.get('name').trim(),
					description: formData.get('description')?.trim() || null,
					benefitAmount: benefitAmount // This should be a number, not null
				};

				if (!data.name) {
					showStatus('Benefit category name is required');
					return;
				}

				const benefitId = benefitIdInput.value;
				const wasEditMode = isEditMode; // Store the mode before making the API call
				let res;

				if (isEditMode && benefitId) {
					// Update existing
					res = await api(`/api/admin/benefit-categories/${benefitId}`, {
						method: 'PUT',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify(data)
					});
				} else {
					// Create new
					res = await api('/api/admin/benefit-categories', {
						method: 'POST',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify(data)
					});
				}

				if (res.ok) {
					showStatus(wasEditMode ? 'Benefit category updated successfully' : 'Benefit category created successfully');
					form.reset();
					benefitIdInput.value = '';
					isEditMode = false;
					submitButton.textContent = 'Create Benefit Category';
					cancelButton.style.display = 'none';
					
					// Update form title
					const formTitle = document.getElementById('benefitFormTitle');
					if (formTitle) {
						formTitle.textContent = 'Create New Benefit Category';
					}
					
					// Refresh the next Benefit ID immediately after creating a new benefit (not after editing)
					if (!wasEditMode) {
						// Small delay to ensure the database has been updated
						setTimeout(() => {
							loadNextBenefitId();
						}, 100);
					}
					
					// Small delay to ensure database is updated before reloading
					setTimeout(() => {
						loadBenefitCategories(searchInput?.value.trim() || '');
						if (typeof loadBenefitCategoriesForAssignment === 'function') {
							loadBenefitCategoriesForAssignment();
						}
					}, 200);
				} else {
					const errorMsg = res.error || res.message || res.payload?.message || 'Unknown error';
					showStatus('Failed to save benefit category: ' + errorMsg);
				}
			});
		}

		// Cancel edit
		if (cancelButton) {
			cancelButton.addEventListener('click', () => {
				form.reset();
				benefitIdInput.value = '';
				isEditMode = false;
				submitButton.textContent = 'Create Benefit Category';
				cancelButton.style.display = 'none';
				
				// Update form title
				const formTitle = document.getElementById('benefitFormTitle');
				if (formTitle) {
					formTitle.textContent = 'Create New Benefit Category';
				}
				
				loadNextBenefitId(); // Refresh next Benefit ID
			});
		}

		// Search functionality
		if (searchButton) {
			searchButton.addEventListener('click', () => {
				const searchTerm = searchInput?.value.trim() || '';
				loadBenefitCategories(searchTerm);
			});
		}

		if (searchInput) {
			searchInput.addEventListener('keypress', (e) => {
				if (e.key === 'Enter') {
					e.preventDefault();
					const searchTerm = searchInput.value.trim();
					loadBenefitCategories(searchTerm);
				}
			});
		}

		// Auto-load categories on page load
		loadBenefitCategories();

		// Benefit Assignment functionality
		const assignForm = document.getElementById('assignBenefitForm');
		const assignEmployeeSelect = document.getElementById('assignEmployeeSelect');
		const assignEmployeeNameInput = document.getElementById('assignEmployeeName');
		const assignBenefitCategorySelect = document.getElementById('assignBenefitCategory');
		const assignmentResultBox = document.getElementById('assignmentResult');
		const assignmentSearchInput = document.getElementById('assignmentSearchInput');
		const assignmentSearchButton = document.getElementById('assignmentSearchButton');

		const updateSelectedEmployeeName = () => {
			if (!assignEmployeeSelect || !assignEmployeeNameInput) return;
			const selectedOption = assignEmployeeSelect.options[assignEmployeeSelect.selectedIndex];
			assignEmployeeNameInput.value = selectedOption?.dataset?.name || '';
		};

		const loadActiveEmployeesForBenefits = async () => {
			if (!assignEmployeeSelect) return;
			const res = await api('/api/admin/users');
			if (res.ok && Array.isArray(res.payload)) {
				const employees = res.payload.filter(user => user.active && user.role === 'EMPLOYEE' && user.employeeId);
				assignEmployeeSelect.innerHTML = '<option value="">Select an active employee</option>';
				employees.forEach(emp => {
					const option = document.createElement('option');
					option.value = emp.employeeId;
					option.textContent = `${emp.employeeId} - ${emp.fullName || emp.username || 'Unnamed'}`;
					option.dataset.name = emp.fullName || emp.username || '';
					assignEmployeeSelect.appendChild(option);
				});
				updateSelectedEmployeeName();
			}
		};
		loadActiveEmployeesForBenefits();

		// Load benefit categories for assignment dropdown
		const loadBenefitCategoriesForAssignment = async () => {
			if (!assignBenefitCategorySelect) return;

			const res = await api('/api/admin/benefit-categories');
			if (res.ok && res.payload) {
				assignBenefitCategorySelect.innerHTML = '<option value="">Select a benefit category</option>';
				res.payload
					.filter(category => category.active !== false)
					.forEach(category => {
					const option = document.createElement('option');
					option.value = category.id;
					option.textContent = `${category.benefitId} - ${category.name} (RM ${category.benefitAmount?.toFixed(2) || '0.00'})`;
					assignBenefitCategorySelect.appendChild(option);
					});
			}
		};

		// Load benefit assignments
		const loadBenefitAssignments = async (searchTerm = '') => {
			if (!assignmentResultBox) return;

			const res = await api('/api/admin/employee-benefits');
			if (res.ok && res.payload) {
				let assignments = res.payload;
				
				// Filter by search term if provided
				if (searchTerm) {
					const searchLower = searchTerm.toLowerCase();
					assignments = assignments.filter(assignment => 
						assignment.employeeId?.toLowerCase().includes(searchLower) ||
						assignment.employeeName?.toLowerCase().includes(searchLower) ||
						assignment.benefitId?.toLowerCase().includes(searchLower) ||
						assignment.benefitName?.toLowerCase().includes(searchLower)
					);
				}

				renderAssignmentTable(assignments);
			} else {
				assignmentResultBox.innerHTML = '<p style="text-align: center; color: #888;">Failed to load benefit assignments.</p>';
			}
		};

		// Render assignment table
		const renderAssignmentTable = (assignments) => {
			if (!assignmentResultBox) return;

			const emptyState = document.getElementById('assignmentEmptyState');
			if (!assignments || assignments.length === 0) {
				assignmentResultBox.innerHTML = '';
				if (emptyState) {
					emptyState.style.display = 'block';
				}
				return;
			}

			if (emptyState) {
				emptyState.style.display = 'none';
			}

			const table = `
				<table class="table-modern">
					<thead>
						<tr>
							<th>Employee ID</th>
							<th>Employee Name</th>
							<th>Benefit ID</th>
							<th>Benefit Name</th>
							<th style="text-align: right;">Benefit Amount (RM)</th>
							<th style="text-align: center;">Actions</th>
						</tr>
					</thead>
					<tbody>
						${assignments.map(assignment => {
							const amount = assignment.benefitAmount != null ? parseFloat(assignment.benefitAmount).toFixed(2) : 'N/A';
							return `
							<tr>
								<td style="font-weight: 500; color: #2f4ceb;">${assignment.employeeId || 'N/A'}</td>
								<td>${assignment.employeeName || 'N/A'}</td>
								<td style="font-weight: 500; color: #2f4ceb;">${assignment.benefitId || 'N/A'}</td>
								<td>${assignment.benefitName || 'N/A'}</td>
								<td style="text-align: right; font-weight: 500;">RM ${amount}</td>
								<td style="text-align: center;">
									<button class="action-btn-modern danger" onclick="window.DashboardApp.unassignBenefit('${assignment.id}')">Remove</button>
								</td>
							</tr>
							`;
						}).join('')}
					</tbody>
				</table>
			`;
			assignmentResultBox.innerHTML = table;
		};

		// Add event listener for employee ID input to fetch employee name
		if (assignEmployeeSelect) {
			assignEmployeeSelect.addEventListener('change', updateSelectedEmployeeName);
		}

		// Assign benefit form submission
		if (assignForm) {
			assignForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(assignForm);
				
				const data = {
					employeeId: formData.get('employeeId').trim(),
					benefitCategoryId: formData.get('benefitCategoryId')
				};

				if (!data.employeeId || !data.benefitCategoryId) {
					showStatus('Please fill in all required fields');
					return;
				}

				const res = await api('/api/admin/employee-benefits/assign', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(data)
				});

				if (res.ok) {
					showStatus('Benefit assigned successfully');
					assignForm.reset();
					if (assignEmployeeSelect) assignEmployeeSelect.value = '';
					updateSelectedEmployeeName();
					loadBenefitAssignments(assignmentSearchInput?.value.trim() || '');
				} else {
					const errorMsg = res.error || res.message || res.payload?.message || 'Unknown error';
					showStatus('Failed to assign benefit: ' + errorMsg);
				}
			});
		}

		// Unassign benefit
		window.DashboardApp.unassignBenefit = async (id) => {
			if (!confirm('Are you sure you want to remove this benefit assignment?')) {
				return;
			}

			const res = await api(`/api/admin/employee-benefits/${id}`, {
				method: 'DELETE'
			});

			if (res.ok) {
				showStatus('Benefit assignment removed successfully');
				loadBenefitAssignments(assignmentSearchInput?.value.trim() || '');
			} else {
				const errorMsg = res.error || res.message || 'Unknown error';
				showStatus('Failed to remove benefit assignment: ' + errorMsg);
			}
		};

		// Search input for assignments (real-time search)
		if (assignmentSearchInput) {
			assignmentSearchInput.addEventListener('input', () => {
				loadBenefitAssignments(assignmentSearchInput.value.trim());
			});
		}

		// Load initial data
		loadBenefitCategoriesForAssignment();
		loadBenefitAssignments();
	};

	const collectUserPayload = (form) => {
		const formData = new FormData(form);
		if (form.active) {
			formData.set('active', form.active.value);
		}
		const payload = Object.fromEntries(formData.entries());
		delete payload.userId;
		// Remove password only for create form (password is auto-generated from IC number for create)
		// For edit form, keep password if provided (user wants to change it)
		const isCreateForm = form.id === 'adminUserCreateForm';
		if (isCreateForm) {
			delete payload.password;
		} else {
			// For edit form, only include password if it's provided and not empty
			if (!payload.password || payload.password.trim() === '') {
				delete payload.password;
			}
		}
		if (payload.active !== undefined) {
			const normalized = String(payload.active).toLowerCase();
			payload.active = normalized === 'true' || normalized === 'active';
		}
		if (payload.role) {
			payload.role = payload.role.toUpperCase();
		}
		if (payload.basicSalary !== undefined && payload.basicSalary !== '') {
			payload.basicSalary = parseFloat(payload.basicSalary);
		}
		// Add +60 prefix to contact number if not empty
		if (payload.contactNumber && payload.contactNumber.trim() !== '') {
			// Remove spaces and dashes, then add +60 prefix
			const cleaned = payload.contactNumber.trim().replace(/[\s\-]/g, '');
			payload.contactNumber = cleaned.startsWith('+60') ? cleaned : '+60' + cleaned;
		}
		// Convert age to integer if present
		if (payload.age !== undefined && payload.age !== '') {
			payload.age = parseInt(payload.age, 10);
		}
		// Handle IC Number and Passport Number
		// IC Number is always required and processed (12 digits, digits only, dashes removed)
		if (payload.icNumber && payload.icNumber.trim() !== '') {
			// Remove all non-digit characters (spaces, dashes, etc.)
			payload.icNumber = payload.icNumber.replace(/[^\d]/g, '');
			// Validate format: must be exactly 12 digits
			if (payload.icNumber.length !== 12 || !/^\d{12}$/.test(payload.icNumber)) {
				throw new Error('IC number must be exactly 12 digits');
			}
		}
		// Handle SOCSO Number (same as IC number: 12 digits, digits only, dashes removed)
		if (payload.socsoNumber && payload.socsoNumber.trim() !== '') {
			// Remove all non-digit characters (spaces, dashes, etc.)
			payload.socsoNumber = payload.socsoNumber.replace(/[^\d]/g, '');
			// Validate format: must be exactly 12 digits
			if (payload.socsoNumber.length !== 12 || !/^\d{12}$/.test(payload.socsoNumber)) {
				throw new Error('SOCSO number must be exactly 12 digits');
			}
		}
		// Handle Passport Number based on nationality (if provided)
		const nationality = payload.nationality || '';
		if (nationality === 'Non-Malaysian' && payload.passportNumber && payload.passportNumber.trim() !== '') {
			payload.passportNumber = payload.passportNumber.trim();
		} else if (nationality === 'Malaysian') {
			// For Malaysian, passport number is optional
			if (payload.passportNumber) {
				payload.passportNumber = payload.passportNumber.trim();
			}
		}
		// Remove spaces and dashes from Bank Account Number
		if (payload.bankAccountNumber && payload.bankAccountNumber.trim() !== '') {
			payload.bankAccountNumber = payload.bankAccountNumber.replace(/[\s\-]/g, '');
		}
		// Convert numberOfChildren to integer
		if (payload.numberOfChildren !== undefined && payload.numberOfChildren !== '') {
			payload.numberOfChildren = parseInt(payload.numberOfChildren, 10);
		}
		// Add +60 prefix to emergency contact number if not empty
		if (payload.emergencyContactNumber && payload.emergencyContactNumber.trim() !== '') {
			const cleaned = payload.emergencyContactNumber.trim().replace(/[\s\-]/g, '');
			payload.emergencyContactNumber = cleaned.startsWith('+60') ? cleaned : '+60' + cleaned;
		}
		// Convert probationPeriodLength to integer if present, otherwise set to null
		if (payload.probationPeriodLength !== undefined && payload.probationPeriodLength !== '') {
			const parsed = parseInt(payload.probationPeriodLength, 10);
			payload.probationPeriodLength = isNaN(parsed) ? null : parsed;
		} else {
			payload.probationPeriodLength = null;
		}
		// reportingManagerId is stored as String (userId), no conversion needed
		if (payload.reportingManagerId !== undefined && payload.reportingManagerId !== '') {
			payload.reportingManagerId = String(payload.reportingManagerId).trim();
		}
		// Bank name is always "Malayan Banking Berhad"
		payload.bankName = 'Malayan Banking Berhad';
		return payload;
	};

	// Function to toggle IC Number / Passport Number fields based on Nationality
	// Check if password is still default and set current password field accordingly
	const checkAndSetCurrentPassword = async (userId, icNumber, passwordInput) => {
		if (!userId || !passwordInput) return;
		
		try {
			const res = await api(`/api/admin/users/${userId}/password-default`);
			if (res.ok && res.payload === true) {
				// Password is still default (IC number)
				passwordInput.value = icNumber || '';
				// Update help text
				const helpText = passwordInput.parentElement?.nextElementSibling;
				if (helpText && helpText.tagName === 'SMALL') {
					helpText.textContent = 'Current password (read-only, default is IC number). Click the eye icon to show/hide.';
				}
			} else {
				// Password has been changed
				passwordInput.value = '••••••••';
				// Update help text
				const helpText = passwordInput.parentElement?.nextElementSibling;
				if (helpText && helpText.tagName === 'SMALL') {
					helpText.textContent = 'Current password (read-only, password has been changed from default). Click the eye icon to show/hide.';
				}
			}
		} catch (error) {
			console.error('Error checking password status:', error);
			// Fallback: assume default password (IC number)
			passwordInput.value = icNumber || '';
		}
	};

	const toggleIdentityFields = (nationality, isCreateForm = false) => {
		// Determine prefix based on form type
		let prefix;
		if (isCreateForm) {
			prefix = 'create';
		} else {
			// Check if it's profile form or edit form
			const profileResidentStatus = document.getElementById('profileResidentStatus');
			if (profileResidentStatus) {
				prefix = 'profile';
			} else {
				prefix = 'edit';
			}
		}
		
		// Try to find elements in modal first, then fallback to document
		const modalId = isCreateForm ? 'createEmployeeModal' : 'editEmployeeModal';
		const modal = document.getElementById(modalId);
		const searchRoot = modal || document;
		
		const icNumberGroup = searchRoot.querySelector(`#${prefix}IcNumberGroup`) || document.getElementById(`${prefix}IcNumberGroup`);
		const passportNumberGroup = searchRoot.querySelector(`#${prefix}PassportNumberGroup`) || document.getElementById(`${prefix}PassportNumberGroup`);
		const icNumberInput = searchRoot.querySelector(`#${prefix}IcNumber`) || document.getElementById(`${prefix}IcNumber`);
		const passportNumberInput = searchRoot.querySelector(`#${prefix}PassportNumber`) || document.getElementById(`${prefix}PassportNumber`);

		console.log(`toggleIdentityFields called: nationality="${nationality}", prefix="${prefix}"`); // Debug log
		console.log(`IC Group found:`, icNumberGroup, `Passport Group found:`, passportNumberGroup); // Debug log

		if (!icNumberGroup || !passportNumberGroup) {
			console.warn(`Could not find identity field groups with prefix: ${prefix}`);
			console.warn(`IC Group: ${icNumberGroup ? 'found' : 'NOT FOUND'}, Passport Group: ${passportNumberGroup ? 'found' : 'NOT FOUND'}`);
			return;
		}

		if (nationality === 'Malaysian') {
			// Malaysian: show IC Number, hide Passport Number
			icNumberGroup.style.display = 'block';
			icNumberGroup.style.visibility = 'visible';
			icNumberGroup.removeAttribute('hidden');
			passportNumberGroup.style.display = 'none';
			passportNumberGroup.style.visibility = 'hidden';
			passportNumberGroup.setAttribute('hidden', 'true');
			if (icNumberInput) {
				icNumberInput.required = true;
			}
			if (passportNumberInput) {
				passportNumberInput.required = false;
				passportNumberInput.value = '';
			}
			console.log('✅ Showing IC Number for Malaysian'); // Debug log
		} else if (nationality === 'Non-Malaysian') {
			// Non-Malaysian: show Passport Number, hide IC Number
			icNumberGroup.style.display = 'none';
			icNumberGroup.style.visibility = 'hidden';
			icNumberGroup.setAttribute('hidden', 'true');
			passportNumberGroup.style.display = 'block';
			passportNumberGroup.style.visibility = 'visible';
			passportNumberGroup.removeAttribute('hidden');
			if (icNumberInput) {
				icNumberInput.required = false;
				icNumberInput.value = '';
			}
			if (passportNumberInput) {
				passportNumberInput.required = true;
			}
			console.log('✅ Showing Passport Number for Non-Malaysian'); // Debug log
		} else {
			// No selection or empty
			icNumberGroup.style.display = 'none';
			icNumberGroup.style.visibility = 'hidden';
			icNumberGroup.setAttribute('hidden', 'true');
			passportNumberGroup.style.display = 'none';
			passportNumberGroup.style.visibility = 'hidden';
			passportNumberGroup.setAttribute('hidden', 'true');
			if (icNumberInput) {
				icNumberInput.required = false;
			}
			if (passportNumberInput) {
				passportNumberInput.required = false;
			}
			console.log('❌ Hiding both fields - no nationality selected'); // Debug log
		}
	};


	const populateAdminUserForm = (form, data) => {
		form.username.value = data.username || '';
		form.employeeId.value = data.employeeId || '';
		// Make employee ID readonly after loading
		form.employeeId.readOnly = true;
		form.employeeId.style.backgroundColor = '#f5f5f5';
		form.employeeId.style.cursor = 'not-allowed';
		form.role.value = data.role || 'EMPLOYEE';
		form.fullName.value = data.fullName || '';
		form.email.value = data.email || '';
		// Set department dropdown value
		if (editDepartmentSelect) {
			editDepartmentSelect.value = data.department || '';
		} else {
			form.department.value = data.department || '';
		}
		form.jobTitle.value = data.jobTitle || '';
		form.basicSalary.value = data.basicSalary || 0;
		// Remove +60 prefix when displaying
		const contactNum = data.contactNumber || '';
		form.contactNumber.value = contactNum.startsWith('+60') ? contactNum.substring(3) : contactNum;
		form.gender.value = data.gender || '';
		form.age.value = data.age || '';
		form.race.value = data.race || '';
		form.religion.value = data.religion || '';
		form.address.value = data.address || '';
		form.maritalStatus.value = data.maritalStatus || '';
		form.bankName.value = 'Malayan Banking Berhad';
		form.bankAccountNumber.value = data.bankAccountNumber || '';
		form.epfNumber.value = data.epfNumber || '';
		// Remove dashes from IC Number for display
		const icNum = data.icNumber || '';
		if (form.icNumber) {
			form.icNumber.value = icNum;
		}
		if (form.passportNumber) {
			form.passportNumber.value = data.passportNumber || '';
		}
		form.taxNumber.value = data.taxNumber || '';
		form.numberOfChildren.value = data.numberOfChildren !== undefined ? data.numberOfChildren : 0;
		form.nationality.value = data.nationality || '';
		form.residentStatus.value = data.residentStatus || '';
		form.spouseWorking.value = data.spouseWorking || '';
		form.password.value = '';
		// Set current password - check if it's still default (IC number) or has been changed
		if (form.currentPassword && data.id) {
			// Check if password is still default by calling API
			checkAndSetCurrentPassword(data.id, icNum, form.currentPassword);
		} else if (form.currentPassword) {
			// Fallback: assume default password (IC number)
			form.currentPassword.value = icNum || '';
		}
		if (form.active) {
			form.active.value = data.active ? 'true' : 'false';
		}
		
		// New fields
		if (form.dateOfBirth) {
			form.dateOfBirth.value = data.dateOfBirth || '';
		}
		if (form.socsoNumber) {
			form.socsoNumber.value = data.socsoNumber || '';
		}
		if (form.dateOfHire) {
			form.dateOfHire.value = data.dateOfHire || '';
		}
		if (form.probationPeriodLength) {
			// Handle 0 value correctly (0 means "No Probation")
			if (data.probationPeriodLength !== undefined && data.probationPeriodLength !== null) {
				form.probationPeriodLength.value = String(data.probationPeriodLength);
			} else {
				form.probationPeriodLength.value = '';
			}
		}
		if (form.employmentType) {
			form.employmentType.value = data.employmentType || '';
		}
		if (form.reportingManagerId) {
			form.reportingManagerId.value = data.reportingManagerId || '';
		}
		if (form.location) {
			form.location.value = data.location || '';
		}
		if (form.emergencyContactName) {
			form.emergencyContactName.value = data.emergencyContactName || '';
		}
		if (form.emergencyContactRelationship) {
			form.emergencyContactRelationship.value = data.emergencyContactRelationship || '';
		}
		if (form.emergencyContactNumber) {
			const emergencyNum = data.emergencyContactNumber || '';
			form.emergencyContactNumber.value = emergencyNum.startsWith('+60') ? emergencyNum.substring(3) : emergencyNum;
		}
		if (form.workPermitType) {
			form.workPermitType.value = data.workPermitType || '';
		}
		if (form.workPermitIssueDate) {
			form.workPermitIssueDate.value = data.workPermitIssueDate || '';
		}
		if (form.workPermitExpiryDate) {
			form.workPermitExpiryDate.value = data.workPermitExpiryDate || '';
		}
		
		// Toggle identity fields based on nationality (IC Number for Malaysian, Passport for Non-Malaysian)
		const nationality = form.nationality?.value || data.nationality || '';
		const isCreateForm = form.id === 'adminUserCreateForm';
		toggleIdentityFields(nationality, isCreateForm);
	};

	const initAdminUsers = () => {
		let adminUserCache = [];
		let selectedUserIds = new Set();

		const refreshButton = document.getElementById('adminUserRefresh');
		const searchInput = document.getElementById('adminUserSearch');
		const statusFilter = document.getElementById('adminUserStatusFilter');
		const dateFromInput = document.getElementById('adminUserDateFrom');
		const dateToInput = document.getElementById('adminUserDateTo');
		const tableBody = document.getElementById('adminUserTableBody');
		const emptyState = document.getElementById('adminUserEmptyState');
		const addScrollButton = document.getElementById('adminUserAddScroll');
		const selectAllCheckbox = document.getElementById('adminUserSelectAll');
		const editSelectedButton = document.getElementById('adminUserEditSelected');
		const deleteSelectedButton = document.getElementById('adminUserDeleteSelected');

		const createForm = document.getElementById('adminUserCreateForm');
		const createDepartmentSelect = document.getElementById('createDepartmentSelect');
		const editDepartmentSelect = document.getElementById('editDepartmentSelect');

		// Modal functions
		const openCreateModal = () => {
			const modal = document.getElementById('createEmployeeModal');
			if (modal) {
				modal.classList.add('show');
				document.body.style.overflow = 'hidden';
				// Initialize nationality field state when modal opens
				setTimeout(() => {
					const createNationality = document.getElementById('createNationality');
					if (createNationality) {
						// Setup listener if not already set
						if (!createNationality.dataset.listenerAttached) {
							createNationality.addEventListener('change', function(e) {
								const nationality = e.target.value;
								console.log('Create form nationality changed to:', nationality);
								toggleIdentityFields(nationality, true);
							});
							createNationality.dataset.listenerAttached = 'true';
						}
						// Initialize if value exists
						if (createNationality.value) {
							toggleIdentityFields(createNationality.value, true);
						}
					}
				}, 50);
			}
		};

		const closeCreateModal = () => {
			const modal = document.getElementById('createEmployeeModal');
			if (modal) {
				modal.classList.remove('show');
				document.body.style.overflow = '';
			}
		};

		const openEditModal = () => {
			const modal = document.getElementById('editEmployeeModal');
			if (modal) {
				modal.classList.add('show');
				document.body.style.overflow = 'hidden';
				// Initialize nationality field state when modal opens
				setTimeout(() => {
					const editNationality = document.getElementById('editNationality');
					if (editNationality) {
						// Setup listener if not already set
						if (!editNationality.dataset.listenerAttached) {
							editNationality.addEventListener('change', function(e) {
								const nationality = e.target.value;
								console.log('Edit form nationality changed to:', nationality);
								toggleIdentityFields(nationality, false);
							});
							editNationality.dataset.listenerAttached = 'true';
						}
						// Initialize if value exists
						if (editNationality.value) {
							toggleIdentityFields(editNationality.value, false);
						}
					}
					// Setup toggle password visibility for current password field
					// Note: The onclick handler is also defined in admin-users.html for direct binding
					const togglePasswordBtn = document.getElementById('toggleEditCurrentPassword');
					const currentPasswordInput = document.getElementById('editCurrentPassword');
					const passwordIcon = document.getElementById('editCurrentPasswordIcon');
					if (togglePasswordBtn && currentPasswordInput && passwordIcon) {
						// Remove existing listener if any by cloning
						if (togglePasswordBtn.dataset.listenerAttached) {
							const newBtn = togglePasswordBtn.cloneNode(true);
							togglePasswordBtn.parentNode.replaceChild(newBtn, togglePasswordBtn);
						}
						
						// Get fresh references
						const btn = document.getElementById('toggleEditCurrentPassword');
						const input = document.getElementById('editCurrentPassword');
						const icon = document.getElementById('editCurrentPasswordIcon');
						
						if (btn && input && icon) {
							// Add event listener as backup (onclick in HTML is primary)
							btn.addEventListener('click', function(e) {
								e.preventDefault();
								e.stopPropagation();
								
								if (input.type === 'password') {
									input.type = 'text';
									icon.textContent = '🙈';
								} else {
									input.type = 'password';
									icon.textContent = '👁️';
								}
							});
							btn.dataset.listenerAttached = 'true';
						}
					}
				}, 50);
			}
		};

		const closeEditModal = () => {
			const modal = document.getElementById('editEmployeeModal');
			if (modal) {
				modal.classList.remove('show');
				document.body.style.overflow = '';
			}
		};

		// Close modal handlers
		const closeCreateBtn = document.getElementById('closeCreateModal');
		if (closeCreateBtn) {
			closeCreateBtn.addEventListener('click', closeCreateModal);
		}
		const closeEditBtn = document.getElementById('closeEditModal');
		if (closeEditBtn) {
			closeEditBtn.addEventListener('click', closeEditModal);
		}

		// Close modal when clicking outside
		document.addEventListener('click', (e) => {
			const createModal = document.getElementById('createEmployeeModal');
			const editModal = document.getElementById('editEmployeeModal');
			if (e.target === createModal) {
				closeCreateModal();
			}
			if (e.target === editModal) {
				closeEditModal();
			}
		});
		const applyCreateFormDefaults = () => {
			if (createForm?.active) {
				createForm.active.value = 'true';
			}
		};
		applyCreateFormDefaults();

		// Load departments and populate dropdowns
		const loadDepartments = async () => {
			const res = await api('/api/admin/departments');
			if (res.ok && res.payload) {
				const departments = res.payload;
				
				// Populate create form dropdown
				if (createDepartmentSelect) {
					createDepartmentSelect.innerHTML = '<option value="">-- Select Department --</option>';
					departments.forEach(dept => {
						const option = document.createElement('option');
						option.value = dept.name;
						option.textContent = dept.name;
						createDepartmentSelect.appendChild(option);
					});
				}
				
				// Populate edit form dropdown
				if (editDepartmentSelect) {
					editDepartmentSelect.innerHTML = '<option value="">-- Select Department --</option>';
					departments.forEach(dept => {
						const option = document.createElement('option');
						option.value = dept.name;
						option.textContent = dept.name;
						editDepartmentSelect.appendChild(option);
					});
				}
			}
		};

		const loadReportingManagers = async () => {
			// Fetch all active employees to populate reporting manager dropdown
			const res = await api('/api/admin/users');
			if (res.ok && res.payload) {
				const employees = res.payload.filter(user => user.active && user.role === 'EMPLOYEE');
				const createReportingManagerSelect = document.getElementById('createReportingManagerSelect');
				const editReportingManagerSelect = document.getElementById('editReportingManagerSelect');
				
				// Populate create form dropdown
				if (createReportingManagerSelect) {
					createReportingManagerSelect.innerHTML = '<option value="">-- Select Reporting Manager --</option>';
					employees.forEach(emp => {
						const option = document.createElement('option');
						option.value = emp.userId;
						option.textContent = `${emp.fullName} (${emp.employeeId || emp.userId})`;
						createReportingManagerSelect.appendChild(option);
					});
				}
				
				// Populate edit form dropdown
				if (editReportingManagerSelect) {
					editReportingManagerSelect.innerHTML = '<option value="">-- Select Reporting Manager --</option>';
					employees.forEach(emp => {
						const option = document.createElement('option');
						option.value = emp.userId;
						option.textContent = `${emp.fullName} (${emp.employeeId || emp.userId})`;
						editReportingManagerSelect.appendChild(option);
					});
				}
			}
		};
		
		// Load departments and reporting managers on initialization
		loadDepartments();
		loadReportingManagers();

		// Setup Nationality change listeners using event delegation (works even if modal is hidden)
		// This ensures the listeners are always active
		document.addEventListener('change', function(e) {
			if (e.target && e.target.id === 'createNationality') {
				const nationality = e.target.value;
				console.log('Create form nationality changed to:', nationality);
				toggleIdentityFields(nationality, true);
			} else if (e.target && e.target.id === 'editNationality') {
				const nationality = e.target.value;
				console.log('Edit form nationality changed to:', nationality);
				toggleIdentityFields(nationality, false);
			}
		});

		// Update bulk action buttons visibility
		const updateBulkActionButtons = () => {
			const hasSelection = selectedUserIds.size > 0;
			if (editSelectedButton) {
				editSelectedButton.style.display = hasSelection ? 'inline-block' : 'none';
			}
			if (deleteSelectedButton) {
				deleteSelectedButton.style.display = hasSelection ? 'inline-block' : 'none';
			}
		};

		const renderUserTable = () => {
			if (!tableBody) return;
			const search = (searchInput?.value || '').trim().toLowerCase();
			const status = statusFilter?.value || 'ALL';
			const dateFrom = dateFromInput?.value || '';
			const dateTo = dateToInput?.value || '';

			const filtered = adminUserCache.filter(user => {
				// Exclude ADMIN role users from the list
				const userRole = user.role ? (typeof user.role === 'string' ? user.role.toUpperCase() : user.role) : '';
				if (userRole === 'ADMIN') {
					return false;
				}
				
				const matchesSearch = search
					? [user.fullName, user.username, user.email, user.department]
						.filter(Boolean)
						.some(field => field.toLowerCase().includes(search))
					: true;
				const matchesStatus = status === 'ALL'
					|| (status === 'ACTIVE' && user.active)
					|| (status === 'INACTIVE' && !user.active);
				
				let matchesDate = true;
				if (user.createdAt && (dateFrom || dateTo)) {
					const userDate = new Date(user.createdAt);
					const userDateOnly = new Date(userDate.getFullYear(), userDate.getMonth(), userDate.getDate());
					
					if (dateFrom) {
						const fromDate = new Date(dateFrom);
						if (userDateOnly < fromDate) {
							matchesDate = false;
						}
					}
					if (dateTo) {
						const toDate = new Date(dateTo);
						if (userDateOnly > toDate) {
							matchesDate = false;
						}
					}
				}
				
				return matchesSearch && matchesStatus && matchesDate;
			});

			tableBody.innerHTML = filtered.map((user, index) => {
				const initials = (user.fullName || user.username || '?')
					.split(' ')
					.filter(Boolean)
					.slice(0, 2)
					.map(part => part[0]?.toUpperCase() ?? '')
					.join('') || '?';
				const created = user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—';
				const isChecked = selectedUserIds.has(user.id);
				return `
					<tr>
						<td>
							<input type="checkbox" class="user-row-checkbox" data-user-id="${user.id}" ${isChecked ? 'checked' : ''}>
						</td>
						<td>${index + 1}</td>
						<td>
							<div class="user-cell-modern">
								<div class="avatar-circle-modern">${initials}</div>
								<div class="user-info-modern">
									<div class="user-name-modern">${user.fullName || user.username}</div>
									<div class="user-sub-modern">${user.email || user.username}</div>
								</div>
							</div>
						</td>
						<td>${user.department || '—'}</td>
						<td><span style="font-weight: 500;">${user.role}</span></td>
						<td>
							<span class="status-pill-modern ${user.active ? 'active' : 'inactive'}">
								${user.active ? 'Active' : 'Inactive'}
							</span>
						</td>
						<td>${created}</td>
						<td>
							<button class="action-btn-modern" data-action="edit-user" data-user-id="${user.id}">Edit</button>
							<button class="action-btn-modern danger" data-action="delete-user" data-user-id="${user.id}">Delete</button>
						</td>
					</tr>
				`;
			}).join('');

			// Update select all checkbox state
			if (selectAllCheckbox) {
				const allFilteredIds = filtered.map(u => u.id);
				const allSelected = allFilteredIds.length > 0 && allFilteredIds.every(id => selectedUserIds.has(id));
				selectAllCheckbox.checked = allSelected;
				selectAllCheckbox.indeterminate = !allSelected && allFilteredIds.some(id => selectedUserIds.has(id));
			}

			// Update bulk action buttons visibility
			updateBulkActionButtons();

			// Re-attach checkbox event listeners after rendering
			attachCheckboxListeners();

			if (emptyState) {
				emptyState.style.display = filtered.length ? 'none' : 'block';
			}
		};

		const fetchUsers = async () => {
			const res = await api('/api/admin/users');
			if (res.ok) {
				adminUserCache = Array.isArray(res.payload) ? res.payload : [];
				renderUserTable();
			} else {
				showStatus('Unable to load employees');
			}
		};

		if (refreshButton) {
			refreshButton.addEventListener('click', fetchUsers);
		}

		if (addScrollButton) {
			addScrollButton.addEventListener('click', openCreateModal);
		}

		if (searchInput) {
			searchInput.addEventListener('input', renderUserTable);
		}

		if (statusFilter) {
			statusFilter.addEventListener('change', renderUserTable);
		}

		if (dateFromInput) {
			dateFromInput.addEventListener('change', renderUserTable);
		}

		if (dateToInput) {
			dateToInput.addEventListener('change', renderUserTable);
		}

		// Helper function to get filtered users
		const getFilteredUsers = () => {
			const search = (searchInput?.value || '').trim().toLowerCase();
			const status = statusFilter?.value || 'ALL';
			const dateFrom = dateFromInput?.value || '';
			const dateTo = dateToInput?.value || '';

			return adminUserCache.filter(user => {
				// Exclude ADMIN role users from the list
				const userRole = user.role ? (typeof user.role === 'string' ? user.role.toUpperCase() : user.role) : '';
				if (userRole === 'ADMIN') {
					return false;
				}
				
				const matchesSearch = search
					? [user.fullName, user.username, user.email, user.department]
						.filter(Boolean)
						.some(field => field.toLowerCase().includes(search))
					: true;
				const matchesStatus = status === 'ALL'
					|| (status === 'ACTIVE' && user.active)
					|| (status === 'INACTIVE' && !user.active);
				
				let matchesDate = true;
				if (user.createdAt && (dateFrom || dateTo)) {
					const userDate = new Date(user.createdAt);
					const userDateOnly = new Date(userDate.getFullYear(), userDate.getMonth(), userDate.getDate());
					
					if (dateFrom) {
						const fromDate = new Date(dateFrom);
						if (userDateOnly < fromDate) {
							matchesDate = false;
						}
					}
					if (dateTo) {
						const toDate = new Date(dateTo);
						if (userDateOnly > toDate) {
							matchesDate = false;
						}
					}
				}
				
				return matchesSearch && matchesStatus && matchesDate;
			});
		};

		// Update select all checkbox state
		const updateSelectAllState = () => {
			if (!selectAllCheckbox) return;
			const filtered = getFilteredUsers();
			const allFilteredIds = filtered.map(u => u.id);
			const allSelected = allFilteredIds.length > 0 && allFilteredIds.every(id => selectedUserIds.has(id));
			selectAllCheckbox.checked = allSelected;
			selectAllCheckbox.indeterminate = !allSelected && allFilteredIds.some(id => selectedUserIds.has(id));
		};

		// Handle select all checkbox
		if (selectAllCheckbox) {
			selectAllCheckbox.addEventListener('change', (e) => {
				const isChecked = e.target.checked;
				const filtered = getFilteredUsers();

				filtered.forEach(user => {
					if (isChecked) {
						selectedUserIds.add(user.id);
					} else {
						selectedUserIds.delete(user.id);
					}
				});

				renderUserTable();
			});
		}

		// Attach checkbox listeners to table body (use event delegation)
		const attachCheckboxListeners = () => {
			// Event delegation is already handled by the document-level listener below
			// This function is kept for consistency but doesn't need to do anything
			// since we use event delegation on the document level
		};

		// Use event delegation for checkbox clicks (works even after table re-renders)
		document.addEventListener('change', (event) => {
			const target = event.target;
			if (target && target.classList && target.classList.contains('user-row-checkbox')) {
				const userId = target.dataset.userId;
				if (target.checked) {
					selectedUserIds.add(userId);
				} else {
					selectedUserIds.delete(userId);
				}
				updateBulkActionButtons();
				updateSelectAllState();
			}
		});

		// Handle edit button clicks (delegated event)
		document.addEventListener('click', (event) => {
			const target = event.target;
			if (!(target instanceof HTMLElement)) {
				return;
			}
			if (target.dataset.action === 'edit-user') {
				const user = adminUserCache.find(u => u.id === target.dataset.userId);
				if (user && editForm) {
					editForm.userId.value = user.id;
					editForm.employeeId.value = user.employeeId || '';
					populateAdminUserForm(editForm, user);
					// Open edit modal
					const editModal = document.getElementById('editEmployeeModal');
					if (editModal) {
						editModal.classList.add('show');
						document.body.style.overflow = 'hidden';
					}
				}
			}
			if (target.dataset.action === 'delete-user') {
				const userId = target.dataset.userId;
				const user = adminUserCache.find(u => u.id === userId);
				const userName = user?.fullName || user?.username || 'this employee';
				const confirmMessage = `Are you sure you want to delete ${userName}? This action cannot be undone.`;
				if (confirm(confirmMessage)) {
					(async () => {
						const res = await api(`/api/admin/users/${userId}`, { method: 'DELETE' });
						if (res.ok) {
							showStatus(`Successfully deleted ${userName}`);
							await fetchUsers();
						} else {
							showStatus(`Failed to delete ${userName}`);
						}
					})();
				}
			}
		});

		// Handle bulk edit
		if (editSelectedButton) {
			editSelectedButton.addEventListener('click', () => {
				const selectedIds = Array.from(selectedUserIds);
				if (selectedIds.length === 0) {
					showStatus('Please select at least one employee to edit');
					return;
				}
				if (selectedIds.length > 1) {
					showStatus('Please select only one employee to edit at a time');
					return;
				}
				const user = adminUserCache.find(u => u.id === selectedIds[0]);
				if (user && editForm) {
					editForm.userId.value = user.id;
					editForm.employeeId.value = user.employeeId || '';
					populateAdminUserForm(editForm, user);
					editForm.scrollIntoView({ behavior: 'smooth' });
					// Clear selection after editing
					selectedUserIds.clear();
					renderUserTable();
				}
			});
		}

		// Handle bulk delete
		if (deleteSelectedButton) {
			deleteSelectedButton.addEventListener('click', async () => {
				const selectedIds = Array.from(selectedUserIds);
				if (selectedIds.length === 0) {
					showStatus('Please select at least one employee to delete');
					return;
				}
				
				const confirmMessage = `Are you sure you want to delete ${selectedIds.length} employee(s)? This action cannot be undone.`;
				if (!confirm(confirmMessage)) {
					return;
				}

				let successCount = 0;
				let failCount = 0;

				for (const userId of selectedIds) {
					const res = await api(`/api/admin/users/${userId}`, { method: 'DELETE' });
					if (res.ok) {
						successCount++;
					} else {
						failCount++;
					}
				}

				if (successCount > 0) {
					showStatus(`Successfully deleted ${successCount} employee(s)`);
				}
				if (failCount > 0) {
					showStatus(`Failed to delete ${failCount} employee(s)`);
				}

				// Clear selection and refresh
				selectedUserIds.clear();
				await fetchUsers();
			});
		}

		if (createForm) {
			createForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				
				// Validate all required fields are filled
				const formData = new FormData(createForm);
				const residentStatus = formData.get('residentStatus') || '';
				
				// Validate IC Number or Passport Number based on resident status
				if (residentStatus === 'Resident') {
					const icNumber = formData.get('icNumber') || '';
					if (!icNumber || !icNumber.trim()) {
						showStatus('IC Number is required for Resident');
						return;
					}
					const cleanedIc = icNumber.replace(/[\s\-]/g, '');
					if (!/^\d{12}$/.test(cleanedIc)) {
						showStatus('IC number must be exactly 12 digits');
						return;
					}
				} else if (residentStatus === 'Non-resident') {
					const passportNumber = formData.get('passportNumber') || '';
					if (!passportNumber || !passportNumber.trim()) {
						showStatus('Passport Number is required for Non-resident');
						return;
					}
				} else {
					showStatus('Please select Resident Status');
					return;
				}

				const requiredFields = [
					{ name: 'username', label: 'Username' },
					{ name: 'role', label: 'Role' },
					{ name: 'fullName', label: 'Full Name' },
					{ name: 'gender', label: 'Gender' },
					{ name: 'age', label: 'Age' },
					{ name: 'race', label: 'Race' },
					{ name: 'religion', label: 'Religion' },
					{ name: 'address', label: 'Address' },
					{ name: 'maritalStatus', label: 'Marital Status' },
					{ name: 'bankName', label: 'Bank Name' },
					{ name: 'bankAccountNumber', label: 'Bank Account Number' },
					{ name: 'epfNumber', label: 'EPF Number' },
					{ name: 'taxNumber', label: 'Tax Number' },
					{ name: 'numberOfChildren', label: 'Number of Children' },
					{ name: 'email', label: 'Email' },
					{ name: 'department', label: 'Department' },
					{ name: 'jobTitle', label: 'Job Title' },
					{ name: 'basicSalary', label: 'Basic Salary' },
					{ name: 'contactNumber', label: 'Contact Number' }
				];
				
				// Check all required fields
				for (const field of requiredFields) {
					const value = formData.get(field.name);
					if (!value || (typeof value === 'string' && value.trim() === '')) {
						showStatus(`${field.label} is required`);
						return;
					}
				}
				
				// Validate dropdown selections (must not be empty string)
				const dropdownFields = [
					{ name: 'role', label: 'Role' },
					{ name: 'gender', label: 'Gender' },
					{ name: 'maritalStatus', label: 'Marital Status' },
					{ name: 'department', label: 'Department' },
					{ name: 'numberOfChildren', label: 'Number of Children' },
					{ name: 'active', label: 'Status' }
				];
				
				for (const field of dropdownFields) {
					const value = formData.get(field.name);
					// numberOfChildren can be 0, but other dropdowns cannot be empty
					if (field.name === 'numberOfChildren') {
						if (value === null || value === undefined || value === '') {
							showStatus(`Please select ${field.label}`);
							return;
						}
						// Allow 0 for numberOfChildren, so continue
						continue;
					}
					// For other dropdowns, value must not be empty
					if (!value || value.trim() === '') {
						showStatus(`Please select a ${field.label}`);
						return;
					}
				}
				
				// Validate age cannot be 0
				const age = formData.get('age');
				if (age) {
					const ageNum = parseInt(age, 10);
					if (isNaN(ageNum) || ageNum <= 0) {
						showStatus('Age must be greater than 0');
						return;
					}
					if (ageNum < 16 || ageNum > 100) {
						showStatus('Age must be between 16 and 100');
						return;
					}
				} else {
					showStatus('Age is required');
					return;
				}
				
				const payload = collectUserPayload(createForm);
				const res = await api('/api/admin/users', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});
				if (res.ok) {
					showStatus('Employee created');
					createForm.reset();
					applyCreateFormDefaults();
					fetchUsers();
					closeCreateModal();
					// Reload next employee ID
					const nextIdRes = await api('/api/admin/next-employee-id');
					if (nextIdRes.ok && document.getElementById('nextEmployeeId')) {
						document.getElementById('nextEmployeeId').value = nextIdRes.payload;
					}
				} else {
					// Extract error message from Spring Boot error response
					// Spring Boot ResponseStatusException can return errors in different formats:
					// Format 1: { "timestamp": "...", "status": 409, "error": "Conflict", "message": "IC number already exists", "path": "..." }
					// Format 2: Just the message string
					// Format 3: { "message": "IC number already exists" }
					let errorMessage = 'Create failed';
					if (typeof res.payload === 'string') {
						errorMessage = res.payload;
					} else if (res.payload && typeof res.payload === 'object') {
						// Try multiple possible fields where the error message might be
						// Priority: message > detail > error (but skip "error" if it's just "Conflict", "Bad Request", etc.)
						errorMessage = res.payload.message || 
									   res.payload.detail ||
									   (res.payload.error && !['Conflict', 'Bad Request', 'Not Found', 'Unauthorized', 'Forbidden'].includes(res.payload.error) ? res.payload.error : null) ||
									   (res.payload.errors && Array.isArray(res.payload.errors) && res.payload.errors[0]?.defaultMessage) ||
									   'Create failed';
					}
					// If we still have a generic error, try to extract from raw text
					if (errorMessage === 'Create failed' && res.rawText) {
						try {
							const parsed = JSON.parse(res.rawText);
							errorMessage = parsed.message || parsed.detail || errorMessage;
						} catch {
							// If raw text is not JSON, use it as the error message
							if (res.rawText && res.rawText.trim() && res.rawText.trim() !== 'Conflict') {
								errorMessage = res.rawText.trim();
							}
						}
					}
					showStatus(errorMessage);
				}
			});
		}

		// Clear form button
		const clearButton = document.getElementById('adminUserClearForm');
		if (clearButton && createForm) {
			clearButton.addEventListener('click', async () => {
				createForm.reset();
				applyCreateFormDefaults();
				// Re-fetch next employee ID
				const nextIdRes = await api('/api/admin/next-employee-id');
				if (nextIdRes.ok && document.getElementById('nextEmployeeId')) {
					document.getElementById('nextEmployeeId').value = nextIdRes.payload;
				}
				showStatus('Form cleared');
			});
		}

		const editForm = document.getElementById('adminUserEditForm');
		const applyEditFormDefaults = () => {
			if (editForm?.active) {
				editForm.active.value = 'true';
			}
		};
		applyEditFormDefaults();
		const fetchButton = document.getElementById('adminUserFetch');
		const deleteButton = document.getElementById('adminUserDelete');

		if (fetchButton && editForm) {
			fetchButton.addEventListener('click', async () => {
				const employeeId = editForm.employeeId.value.trim();
				if (!employeeId) {
					return showStatus('Employee ID required');
				}
				const res = await api(`/api/admin/users/by-employee-id/${employeeId}`);
				if (res.ok) {
					editForm.userId.value = res.payload.id;
					populateAdminUserForm(editForm, res.payload);
					if (editForm.active) {
						editForm.active.value = res.payload.active ? 'true' : 'false';
					}
					renderUserTable();
				} else {
					showStatus('Unable to load employee');
				}
			});
		}

		if (editForm) {
			editForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				const userId = editForm.userId.value.trim();
				if (!userId) {
					return showStatus('User ID required');
				}
				const payload = collectUserPayload(editForm);
				const res = await api(`/api/admin/users/${userId}`, {
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});
				if (res.ok) {
					showStatus('Employee updated');
					fetchUsers();
					closeEditModal();
				} else {
					// Extract error message from Spring Boot error response
					// Spring Boot ResponseStatusException can return errors in different formats:
					// Format 1: { "timestamp": "...", "status": 409, "error": "Conflict", "message": "IC number already exists", "path": "..." }
					// Format 2: Just the message string
					// Format 3: { "message": "IC number already exists" }
					let errorMessage = 'Update failed';
					if (typeof res.payload === 'string') {
						errorMessage = res.payload;
					} else if (res.payload && typeof res.payload === 'object') {
						// Try multiple possible fields where the error message might be
						// Priority: message > detail > error (but skip "error" if it's just "Conflict", "Bad Request", etc.)
						errorMessage = res.payload.message || 
									   res.payload.detail ||
									   (res.payload.error && !['Conflict', 'Bad Request', 'Not Found', 'Unauthorized', 'Forbidden'].includes(res.payload.error) ? res.payload.error : null) ||
									   (res.payload.errors && Array.isArray(res.payload.errors) && res.payload.errors[0]?.defaultMessage) ||
									   'Update failed';
					}
					// If we still have a generic error, try to extract from raw text
					if (errorMessage === 'Update failed' && res.rawText) {
						try {
							const parsed = JSON.parse(res.rawText);
							errorMessage = parsed.message || parsed.detail || errorMessage;
						} catch {
							// If raw text is not JSON, use it as the error message
							if (res.rawText && res.rawText.trim() && res.rawText.trim() !== 'Conflict') {
								errorMessage = res.rawText.trim();
							}
						}
					}
					showStatus(errorMessage);
				}
			});
		}

		if (deleteButton && editForm) {
			deleteButton.addEventListener('click', async () => {
				const userId = editForm.userId.value.trim();
				if (!userId) {
					return showStatus('User ID required');
				}
				const confirmed = confirm('Delete this employee?');
				if (!confirmed) {
					return;
				}
				const res = await api(`/api/admin/users/${userId}`, {
					method: 'DELETE'
				});
				showStatus(res.ok ? 'Employee deleted' : 'Delete failed');
				if (res.ok) {
					editForm.reset();
					applyEditFormDefaults();
					fetchUsers();
					closeEditModal();
					// Refresh next employee ID
					const nextIdRes = await api('/api/admin/next-employee-id');
					if (nextIdRes.ok && document.getElementById('nextEmployeeId')) {
						document.getElementById('nextEmployeeId').value = nextIdRes.payload;
					}
				}
			});
		}


		fetchUsers();
	};

	const guardRole = async (requiredRole) => {
		const hasSession = await loadSession();
		if (!hasSession) {
			window.location.href = '/login.html';
			return;
		}
		if (requiredRole && session.role !== requiredRole) {
			window.location.href = '/login.html';
		}
	};

	const initAdminKPIs = () => {
		const form = document.getElementById('kpiForm');
		const resultBox = document.getElementById('kpiResult');
		const searchInput = document.getElementById('kpiSearchInput');
		const searchButton = document.getElementById('kpiSearchButton');
		const refreshButton = document.getElementById('kpiRefreshButton');
		const submitButton = document.getElementById('kpiSubmitButton');
		const cancelButton = document.getElementById('kpiCancelButton');
		const kpiIdInput = document.getElementById('kpiId');
		const bonusAmountInput = document.getElementById('bonusAmountInput');
		const dueDateInput = document.getElementById('kpiDueDateInput');

		let isEditMode = false;

		const getKpiStatusBadge = (active) => {
			const isActive = active !== false;
			const statusClass = isActive ? 'active' : 'inactive';
			return `<span class="status-pill-modern ${statusClass}">${isActive ? 'Active' : 'Inactive'}</span>`;
		};

		// Load next KPI ID
		const loadNextKpiId = async () => {
			const displayKpiId = document.getElementById('displayKpiId');
			if (!displayKpiId) return;

			const res = await api('/api/admin/kpis/next-kpi-id');
			if (res.ok && res.payload) {
				displayKpiId.value = res.payload;
			} else {
				// Fallback to K001 if API call fails
				displayKpiId.value = 'K001';
			}
		};

		const getTodayDateString = () => {
			const today = new Date();
			const yyyy = today.getFullYear();
			const mm = String(today.getMonth() + 1).padStart(2, '0');
			const dd = String(today.getDate()).padStart(2, '0');
			return `${yyyy}-${mm}-${dd}`;
		};

		const enforceDueDateMin = () => {
			if (!dueDateInput) return;
			dueDateInput.min = getTodayDateString();
		};

		// Load next KPI ID on page load
		loadNextKpiId();
		enforceDueDateMin();

		// Real-time validation for measurable value input
		const measurableValueInput = document.getElementById('measurableValueInput');
		if (measurableValueInput) {
			measurableValueInput.addEventListener('input', (e) => {
				const value = e.target.value.trim();
				// Remove leading zeros (except for single "0" which will be caught by min="1")
				if (value.length > 1 && value.startsWith('0')) {
					e.target.value = value.replace(/^0+/, '');
				}
			});

			measurableValueInput.addEventListener('blur', (e) => {
				const value = e.target.value.trim();
				if (value && (isNaN(value) || parseFloat(value) <= 0)) {
					e.target.setCustomValidity('Measurable value must be a number greater than 0');
				} else {
					e.target.setCustomValidity('');
				}
			});
		}

		// Real-time validation for bonus amount input
		if (bonusAmountInput) {
			bonusAmountInput.addEventListener('input', (e) => {
				const value = e.target.value.trim();
				// Remove leading zeros (except for "0" or "0.xx")
				if (value.length > 1 && value.startsWith('0') && !value.startsWith('0.')) {
					e.target.value = value.replace(/^0+/, '');
				}
			});

			bonusAmountInput.addEventListener('blur', (e) => {
				const value = e.target.value.trim();
				if (value && parseFloat(value) <= 0) {
					e.target.setCustomValidity('Bonus amount must be greater than 0');
				} else {
					e.target.setCustomValidity('');
				}
			});
		}

		// Load and display KPI categories
		const loadKPICategories = async (searchTerm = '') => {
			if (!resultBox) return;

			const url = searchTerm 
				? `/api/admin/kpis/search?search=${encodeURIComponent(searchTerm)}`
				: '/api/admin/kpis';
			
			const res = await api(url);
			if (res.ok && res.payload) {
				renderKPITable(res.payload);
			} else {
				resultBox.innerHTML = '<p style="text-align: center; color: #888;">Failed to load KPI categories.</p>';
			}
		};

		// Render KPI categories table
		const renderKPITable = (kpis) => {
			if (!resultBox) return;

			if (!kpis || kpis.length === 0) {
				resultBox.innerHTML = '<div class="empty-state-modern">No KPI categories found.</div>';
				return;
			}

			const table = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>KPI ID</th>
								<th>Name</th>
								<th>Description</th>
								<th>Measurable Value</th>
								<th>Due Date</th>
								<th style="text-align: right;">Bonus Amount (RM)</th>
								<th>Status</th>
								<th style="text-align: center; width: 220px;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${kpis.map(kpi => {
								const kpiId = kpi.kpiId || 'N/A';
								const measurableValue = kpi.measurableValue || 'N/A';
								const isActive = kpi.active !== false;
								// Format due date properly
								let dueDate = 'N/A';
								if (kpi.dueDate) {
									if (typeof kpi.dueDate === 'string') {
										// If it's already a string in YYYY-MM-DD format, parse it
										const dateParts = kpi.dueDate.split('-');
										if (dateParts.length === 3) {
											dueDate = new Date(parseInt(dateParts[0]), parseInt(dateParts[1]) - 1, parseInt(dateParts[2])).toLocaleDateString();
										} else {
											dueDate = new Date(kpi.dueDate).toLocaleDateString();
										}
									} else {
										dueDate = new Date(kpi.dueDate).toLocaleDateString();
									}
								}
								const bonusAmount = kpi.bonusAmount != null ? parseFloat(kpi.bonusAmount).toFixed(2) : 'N/A';
								return `
								<tr>
									<td style="font-weight: 500; color: #2f4ceb;">${kpiId}</td>
									<td style="font-weight: 500;">${kpi.name || 'N/A'}</td>
									<td style="color: #666;">${kpi.description || 'No description'}</td>
									<td>${measurableValue}</td>
									<td>${dueDate}</td>
									<td style="text-align: right; font-weight: 500;">RM ${bonusAmount}</td>
									<td>${getKpiStatusBadge(isActive)}</td>
									<td style="text-align: center;">
										<div style="display: flex; gap: 8px; justify-content: center;">
											<button onclick="window.DashboardApp.editKPI('${kpi.id}')" class="action-btn-modern">
												Edit
											</button>
											<button onclick="window.DashboardApp.toggleKpiStatus('${kpi.id}', ${isActive ? 'false' : 'true'})" 
													class="action-btn-modern ${isActive ? 'danger' : ''}" style="${isActive ? '' : 'color: #198754;'}">
												${isActive ? 'Deactivate' : 'Activate'}
											</button>
										</div>
									</td>
								</tr>
								`;
							}).join('')}
						</tbody>
					</table>
				</div>
			`;
			resultBox.innerHTML = table;
		};

		// Edit KPI category
		window.DashboardApp = window.DashboardApp || {};
		window.DashboardApp.editKPI = async (id) => {
			if (!form) return;

			const res = await api(`/api/admin/kpis/${id}`);
			if (res.ok && res.payload) {
				const kpi = res.payload;
				form.name.value = kpi.name || '';
				form.description.value = kpi.description || '';
				form.measurableValue.value = kpi.measurableValue || '';
				if (kpi.dueDate) {
					// Handle date - could be ISO string (YYYY-MM-DD) or Date object
					if (typeof kpi.dueDate === 'string') {
						// If it's already in YYYY-MM-DD format, use it directly
						form.dueDate.value = kpi.dueDate;
					} else {
						// Format date as YYYY-MM-DD for date input
						const dueDate = new Date(kpi.dueDate);
						const year = dueDate.getFullYear();
						const month = String(dueDate.getMonth() + 1).padStart(2, '0');
						const day = String(dueDate.getDate()).padStart(2, '0');
						form.dueDate.value = `${year}-${month}-${day}`;
					}
				} else {
					form.dueDate.value = '';
				}
				enforceDueDateMin();
				form.bonusAmount.value = kpi.bonusAmount != null ? kpi.bonusAmount : '';
				kpiIdInput.value = kpi.id;
				
				// Show KPI ID display in edit mode
				const displayKpiId = document.getElementById('displayKpiId');
				if (displayKpiId) {
					displayKpiId.value = kpi.kpiId || 'N/A';
				}
				
				isEditMode = true;
				submitButton.textContent = 'Update KPI Category';
				cancelButton.style.display = 'inline-block';
				
				form.scrollIntoView({ behavior: 'smooth', block: 'center' });
			} else {
				showStatus('Failed to load KPI category');
			}
		};

		window.DashboardApp.toggleKpiStatus = async (id, nextActive) => {
			if (!nextActive && !confirm('Deactivate this KPI? Assignments will remain but it will be hidden from new assignments.')) {
				return;
			}
			const res = await api(`/api/admin/kpis/${id}/status`, {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ active: nextActive })
			});
			if (res.ok) {
				showStatus(nextActive ? 'KPI activated successfully' : 'KPI deactivated successfully');
				loadKPICategories(searchInput?.value.trim() || '');
				if (typeof loadKPICategoriesForAssignment === 'function') {
					loadKPICategoriesForAssignment();
				}
			} else {
				const errorMsg = res.error || res.message || res.payload?.message || 'Unknown error';
				showStatus('Failed to update KPI status: ' + errorMsg);
			}
		};

		// Form submission
		if (form) {
		form.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(form);

				// Validate bonus amount
				const bonusAmountValue = formData.get('bonusAmount');
				if (!bonusAmountValue || bonusAmountValue.trim() === '') {
					showStatus('Bonus amount is required');
					return;
				}

				// Check if starts with 0 but is not exactly "0" or "0.xx"
				const trimmedBonusValue = bonusAmountValue.trim();
				if (trimmedBonusValue.length > 1 && trimmedBonusValue.startsWith('0') && !trimmedBonusValue.startsWith('0.')) {
					showStatus('Bonus amount cannot start with 0');
					return;
				}

				const bonusAmount = parseFloat(trimmedBonusValue);
				if (isNaN(bonusAmount)) {
					showStatus('Bonus amount must be a valid number');
					return;
				}

				if (bonusAmount <= 0) {
					showStatus('Bonus amount must be greater than 0');
					return;
				}

				// Validate measurable value
				const measurableValue = formData.get('measurableValue');
				if (!measurableValue || measurableValue.trim() === '') {
					showStatus('Measurable value is required');
					return;
				}

				// Check if starts with 0 but is not exactly "0"
				const trimmedMeasurableValue = measurableValue.trim();
				if (trimmedMeasurableValue.length > 1 && trimmedMeasurableValue.startsWith('0')) {
					showStatus('Measurable value cannot start with 0');
					return;
				}

				const measurableValueNum = parseFloat(trimmedMeasurableValue);
				if (isNaN(measurableValueNum)) {
					showStatus('Measurable value must be a valid number');
					return;
				}

				if (measurableValueNum <= 0) {
					showStatus('Measurable value must be greater than 0');
					return;
				}

				// Ensure it's an integer (digits only)
				if (!/^\d+$/.test(trimmedMeasurableValue)) {
					showStatus('Measurable value must contain digits only');
					return;
				}

				// Validate due date
			const dueDate = formData.get('dueDate');
				if (!dueDate || dueDate.trim() === '') {
					showStatus('Due date is required');
					return;
				}
			const today = new Date();
			today.setHours(0, 0, 0, 0);
			const selectedDueDate = new Date(`${dueDate.trim()}T00:00:00`);
			if (selectedDueDate < today) {
				showStatus('Due date cannot be in the past');
				return;
			}

				const data = {
					name: formData.get('name').trim(),
					description: formData.get('description')?.trim() || null,
					measurableValue: trimmedMeasurableValue,
					dueDate: dueDate.trim(),
					bonusAmount: bonusAmount
				};

				if (!data.name) {
					showStatus('KPI name is required');
					return;
				}

				const kpiId = kpiIdInput.value;
				const wasEditMode = isEditMode; // Store the mode before making the API call
				let res;

				if (isEditMode && kpiId) {
					// Update existing
					res = await api(`/api/admin/kpis/${kpiId}`, {
						method: 'PUT',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify(data)
					});
				} else {
					// Create new
					res = await api('/api/admin/kpis', {
						method: 'POST',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify(data)
					});
				}

				if (res.ok) {
					showStatus(wasEditMode ? 'KPI category updated successfully' : 'KPI category created successfully');
					form.reset();
					kpiIdInput.value = '';
					isEditMode = false;
					submitButton.textContent = 'Create KPI Category';
					cancelButton.style.display = 'none';
					
					// Refresh the next KPI ID immediately after creating a new KPI (not after editing)
					if (!wasEditMode) {
						// Small delay to ensure the database has been updated
						setTimeout(() => {
							loadNextKpiId();
						}, 100);
					}
					
					// Small delay to ensure database is updated before reloading
					setTimeout(() => {
						loadKPICategories(searchInput?.value.trim() || '');
						if (typeof loadKPICategoriesForAssignment === 'function') {
							loadKPICategoriesForAssignment();
						}
					}, 200);
				} else {
					const errorMsg = res.error || res.message || res.payload?.message || 'Unknown error';
					showStatus('Failed to save KPI category: ' + errorMsg);
				}
			});
		}

		// Cancel edit
		if (cancelButton) {
			cancelButton.addEventListener('click', () => {
				form.reset();
				kpiIdInput.value = '';
				isEditMode = false;
				submitButton.textContent = 'Create KPI Category';
				cancelButton.style.display = 'none';
				loadNextKpiId(); // Refresh next KPI ID
			});
		}

		// Search functionality
		if (searchButton) {
			searchButton.addEventListener('click', () => {
				const searchTerm = searchInput?.value.trim() || '';
				loadKPICategories(searchTerm);
			});
		}

		if (searchInput) {
			searchInput.addEventListener('keypress', (e) => {
				if (e.key === 'Enter') {
					e.preventDefault();
					const searchTerm = searchInput.value.trim();
					loadKPICategories(searchTerm);
				}
			});
		}

		// Refresh functionality
		if (refreshButton) {
			refreshButton.addEventListener('click', () => {
				searchInput.value = '';
				loadKPICategories();
			});
		}

		// Initial load
		loadKPICategories();

		// KPI Assignment functionality
		const assignForm = document.getElementById('assignKPIForm');
		const assignmentTypeSelect = document.getElementById('assignmentType');
		const individualFields = document.getElementById('individualAssignmentFields');
		const bulkFields = document.getElementById('bulkAssignmentFields');
		const assignEmployeeSelect = document.getElementById('assignEmployeeSelect');
		const assignEmployeeName = document.getElementById('assignEmployeeName');
		const assignDepartmentName = document.getElementById('assignDepartmentName');
		const assignKpiCategory = document.getElementById('assignKpiCategory');
		const assignmentResultBox = document.getElementById('kpiAssignmentResult');
		const assignmentSearchInput = document.getElementById('kpiAssignmentSearchInput');
		const assignmentSearchButton = document.getElementById('kpiAssignmentSearchButton');
		const assignmentRefreshButton = document.getElementById('kpiAssignmentRefreshButton');
		const assignKPIClearButton = document.getElementById('assignKPIClearButton');
		const dueAlertBanner = document.getElementById('kpiDueAlert');
		const evaluationModal = document.getElementById('kpiEvaluationModal');
		const evaluationModalTitle = document.getElementById('evaluationModalTitle');
		const evaluationAssignmentIdInput = document.getElementById('evaluationAssignmentId');
		const evaluationEmployeeText = document.getElementById('evaluationEmployeeText');
		const evaluationTargetText = document.getElementById('evaluationTargetText');
		const evaluationProgressText = document.getElementById('evaluationProgressText');
		const evaluationStatusBadge = document.getElementById('evaluationStatusBadge');
		const evaluationDueWarning = document.getElementById('evaluationDueWarning');
		const evaluationCloseButton = document.getElementById('closeEvaluationModal');
		const evaluationButtons = document.querySelectorAll('#kpiEvaluationForm [data-eval-status]');

		const adminAssignmentCache = new Map();
		const sanitizeAdminText = (text) => {
			const div = document.createElement('div');
			div.textContent = text ?? '';
			return div.innerHTML;
		};
		const getAdminStatusBadge = (status) => {
			const normalized = (status || 'PENDING').toUpperCase();
			const statusClass = normalized === 'COMPLETED' ? 'completed' : normalized === 'INCOMPLETE' ? 'incomplete' : 'pending';
			const label = normalized === 'COMPLETED' ? 'Completed' : normalized === 'INCOMPLETE' ? 'Incomplete' : 'Pending';
			return `<span class="status-pill-modern ${statusClass}">${label}</span>`;
		};
		const isDueDateReached = (value) => {
			if (!value) return false;
			const today = new Date();
			today.setHours(0, 0, 0, 0);
			const dueDate = new Date(value);
			dueDate.setHours(0, 0, 0, 0);
			return today.getTime() >= dueDate.getTime();
		};
		const isPendingStatus = (assignment) => ((assignment?.status || 'PENDING').toUpperCase() === 'PENDING');
		const meetsTarget = (assignment) => {
			const target = parseFloat(assignment.measurableValue || '0') || 0;
			const current = assignment.currentProgressValue != null ? assignment.currentProgressValue : 0;
			return target > 0 && current >= target;
		};
		const updateDueBanner = (assignments) => {
			if (!dueAlertBanner) return;
			const pendingDue = assignments.filter(a =>
				isDueDateReached(a.dueDate) &&
				isPendingStatus(a)
			).length;
			if (pendingDue > 0) {
				dueAlertBanner.style.display = 'block';
				dueAlertBanner.textContent = `${pendingDue} KPI assignment${pendingDue > 1 ? 's' : ''} reached the due date and await evaluation.`;
			} else {
				dueAlertBanner.style.display = 'none';
			}
		};

		const closeEvaluationModal = () => {
			if (evaluationModal) {
				evaluationModal.style.display = 'none';
				evaluationModal.style.visibility = 'hidden';
				document.body.style.overflow = ''; // Restore scrolling
			}
			if (evaluationAssignmentIdInput) {
				evaluationAssignmentIdInput.value = '';
			}
		};

		const openKpiEvaluationModal = async (assignmentId) => {
			console.log('openKpiEvaluationModal called with assignmentId:', assignmentId); // Debug log
			if (!evaluationModal) {
				console.error('Evaluation modal not found');
				showStatus('Evaluation modal not found. Please refresh the page.');
				return;
			}
			if (!assignmentId) {
				console.error('Assignment ID is missing');
				showStatus('Assignment ID is missing.');
				return;
			}
			let assignment = adminAssignmentCache.get(assignmentId);
			console.log('Assignment from cache:', assignment); // Debug log
			console.log('Cache contents:', Array.from(adminAssignmentCache.keys())); // Debug log
			
			// If not in cache, try to fetch from API
			if (!assignment) {
				console.warn('Assignment not found in cache, attempting to fetch from API...');
				try {
					const res = await api(`/api/admin/kpi-assignments`);
					if (res.ok && Array.isArray(res.payload)) {
						assignment = res.payload.find(a => a.id === assignmentId);
						if (assignment) {
							adminAssignmentCache.set(assignmentId, assignment);
							console.log('Assignment fetched from API and cached');
						}
					}
				} catch (error) {
					console.error('Error fetching assignment:', error);
				}
			}
			
			if (!assignment) {
				console.error('Assignment not found in cache or API. Cache size:', adminAssignmentCache.size);
				showStatus('Unable to load assignment details. Please refresh the page and try again.');
				return;
			}
			if (!isPendingStatus(assignment)) {
				showStatus('This KPI has already been evaluated.');
				return;
			}
			if (evaluationAssignmentIdInput) evaluationAssignmentIdInput.value = assignmentId;
			if (evaluationModalTitle) evaluationModalTitle.textContent = `Evaluate ${assignment.kpiName || assignment.kpiId || 'KPI'}`;
			if (evaluationEmployeeText) evaluationEmployeeText.textContent = `${assignment.employeeName || 'Employee'} (${assignment.employeeId || 'N/A'})`;
			if (evaluationTargetText) evaluationTargetText.textContent = assignment.measurableValue || 'N/A';
			if (evaluationProgressText) evaluationProgressText.textContent = `${assignment.currentProgressValue != null ? assignment.currentProgressValue : 0} / ${assignment.measurableValue || '0'}`;
			if (evaluationStatusBadge) evaluationStatusBadge.innerHTML = getAdminStatusBadge(assignment.status);
			const dueReached = isDueDateReached(assignment.dueDate);
			if (evaluationDueWarning) {
				evaluationDueWarning.style.display = dueReached ? 'none' : 'block';
			}
			// Re-query buttons after form might have been cloned
			const currentEvaluationButtons = document.querySelectorAll('#kpiEvaluationForm [data-eval-status]');
			if (currentEvaluationButtons && currentEvaluationButtons.length) {
				currentEvaluationButtons.forEach((button) => {
					const desiredStatus = button.dataset.evalStatus;
					const meets = meetsTarget(assignment);
					if (!dueReached) {
						button.disabled = true;
						button.style.pointerEvents = 'none';
						button.style.opacity = '0.5';
						button.title = 'Evaluation unlocks after the due date';
					} else if (desiredStatus === 'COMPLETED' && !meets) {
						button.disabled = true;
						button.style.pointerEvents = 'none';
						button.style.opacity = '0.5';
						button.title = 'Progress has not met the target value';
					} else {
						button.disabled = false;
						button.style.pointerEvents = 'auto';
						button.style.opacity = '1';
						button.removeAttribute('title');
					}
				});
			}
			
			// Ensure Close button is always enabled
			const closeBtn = document.getElementById('closeEvaluationModal');
			if (closeBtn) {
				closeBtn.disabled = false;
				closeBtn.style.pointerEvents = 'auto';
				closeBtn.style.opacity = '1';
			}
			
			// Show the modal
			if (evaluationModal) {
				evaluationModal.style.display = 'flex';
				evaluationModal.style.visibility = 'visible';
				evaluationModal.style.opacity = '1';
				document.body.style.overflow = 'hidden'; // Prevent background scrolling
				console.log('Evaluation modal opened and displayed'); // Debug log
			} else {
				console.error('evaluationModal element is null');
			}
			
			// Re-attach button handlers in case they were lost
			setTimeout(() => {
				attachEvaluationButtonHandlers();
			}, 100);
		};

		window.DashboardApp = window.DashboardApp || {};
		window.DashboardApp.openKpiEvaluationModal = openKpiEvaluationModal;

		if (evaluationCloseButton) {
			evaluationCloseButton.addEventListener('click', closeEvaluationModal);
		}

		if (evaluationModal) {
			evaluationModal.addEventListener('click', (event) => {
				if (event.target === evaluationModal) {
					closeEvaluationModal();
				}
			});
		}

		const submitEvaluation = async (status) => {
			console.log('submitEvaluation called with status:', status); // Debug log
			if (!evaluationAssignmentIdInput || !evaluationAssignmentIdInput.value) {
				console.error('No assignment ID found');
				showStatus('No KPI assignment selected for evaluation.');
				return;
			}
			const assignmentId = evaluationAssignmentIdInput.value;
			console.log('Submitting evaluation for assignment:', assignmentId, 'with status:', status);
			showStatus('Submitting evaluation...');
			
			const payload = { status };
			try {
				const res = await api(`/api/admin/kpi-assignments/${assignmentId}/status`, {
					method: 'PATCH',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(payload)
				});
				console.log('Evaluation API response:', res); // Debug log
				if (res.ok || res.status === 200) {
					showStatus(`KPI marked as ${status.toLowerCase()}`);
					closeEvaluationModal();
					loadKPIAssignments(assignmentSearchInput?.value.trim() || '');
				} else {
					const errorMsg = res.error || res.message || res.payload?.message || res.rawText || 'Failed to update KPI status';
					console.error('Evaluation failed:', res);
					showStatus('Failed: ' + errorMsg);
				}
			} catch (error) {
				console.error('Error submitting evaluation:', error);
				showStatus('Error: ' + error.message);
			}
		};

		// Attach event listeners to evaluation buttons - re-attach every time modal opens
		const attachEvaluationButtonHandlers = () => {
			console.log('Attaching evaluation button handlers...');
			
			// Always re-query buttons to get fresh references
			const completeButton = document.getElementById('evaluationCompleteButton');
			const incompleteButton = document.getElementById('evaluationIncompleteButton');
			const closeButton = document.getElementById('closeEvaluationModal');
			
			console.log('Found buttons:', {
				complete: !!completeButton,
				incomplete: !!incompleteButton,
				close: !!closeButton
			});
			
			// Remove old listeners by cloning buttons
			if (completeButton) {
				const newComplete = completeButton.cloneNode(true);
				completeButton.parentNode.replaceChild(newComplete, completeButton);
				newComplete.addEventListener('click', async (e) => {
					e.preventDefault();
					e.stopPropagation();
					e.stopImmediatePropagation();
					console.log('Complete button clicked!', 'disabled:', newComplete.disabled);
					if (!newComplete.disabled) {
						await submitEvaluation('COMPLETED');
					} else {
						console.log('Complete button is disabled');
					}
				});
			}
			
			if (incompleteButton) {
				const newIncomplete = incompleteButton.cloneNode(true);
				incompleteButton.parentNode.replaceChild(newIncomplete, incompleteButton);
				newIncomplete.addEventListener('click', async (e) => {
					e.preventDefault();
					e.stopPropagation();
					e.stopImmediatePropagation();
					console.log('Incomplete button clicked!', 'disabled:', newIncomplete.disabled);
					if (!newIncomplete.disabled) {
						await submitEvaluation('INCOMPLETE');
					} else {
						console.log('Incomplete button is disabled');
					}
				});
			}
			
			if (closeButton) {
				const newClose = closeButton.cloneNode(true);
				closeButton.parentNode.replaceChild(newClose, closeButton);
				newClose.addEventListener('click', (e) => {
					e.preventDefault();
					e.stopPropagation();
					e.stopImmediatePropagation();
					console.log('Close button clicked!');
					closeEvaluationModal();
				});
			}
		};
		
		// Attach handlers immediately
		attachEvaluationButtonHandlers();

		const resetAssignmentState = () => {
			if (assignForm) {
				assignForm.reset();
			}
			if (assignEmployeeSelect) {
				assignEmployeeSelect.value = '';
				assignEmployeeSelect.required = false;
			}
			if (assignEmployeeName) {
				assignEmployeeName.value = '';
			}
			if (assignmentTypeSelect) {
				assignmentTypeSelect.value = '';
			}
			if (individualFields) {
				individualFields.style.display = 'none';
			}
			if (bulkFields) {
				bulkFields.style.display = 'none';
			}
			if (assignDepartmentName) {
				assignDepartmentName.required = false;
			}
		};

		// Ensure fields start hidden
		resetAssignmentState();

		// Toggle assignment type fields
		if (assignmentTypeSelect) {
			assignmentTypeSelect.addEventListener('change', (e) => {
				const type = e.target.value;
				if (type === 'individual') {
					if (individualFields) individualFields.style.display = 'block';
					if (bulkFields) bulkFields.style.display = 'none';
					if (assignEmployeeSelect) assignEmployeeSelect.required = true;
					if (assignDepartmentName) assignDepartmentName.required = false;
				} else if (type === 'bulk') {
					if (individualFields) individualFields.style.display = 'none';
					if (bulkFields) bulkFields.style.display = 'block';
					if (assignEmployeeSelect) assignEmployeeSelect.required = false;
					if (assignDepartmentName) assignDepartmentName.required = true;
				} else {
					if (individualFields) individualFields.style.display = 'none';
					if (bulkFields) bulkFields.style.display = 'none';
					if (assignEmployeeSelect) assignEmployeeSelect.required = false;
					if (assignDepartmentName) assignDepartmentName.required = false;
				}
			});
		}

		if (assignKPIClearButton) {
			assignKPIClearButton.addEventListener('click', () => {
				resetAssignmentState();
			});
		}

		// Load departments for bulk assignment
		const loadDepartments = async () => {
			if (!assignDepartmentName) return;
			const res = await api('/api/admin/departments');
			if (res.ok && res.payload) {
				assignDepartmentName.innerHTML = '<option value="">Select a department</option>';
				res.payload.forEach(dept => {
					const option = document.createElement('option');
					option.value = dept.name;
					option.textContent = dept.name;
					assignDepartmentName.appendChild(option);
				});
			}
		};

		const updateSelectedAssignmentEmployeeName = () => {
			if (!assignEmployeeSelect || !assignEmployeeName) return;
			const selectedOption = assignEmployeeSelect.options[assignEmployeeSelect.selectedIndex];
			assignEmployeeName.value = selectedOption?.dataset?.name || '';
		};

		const loadActiveEmployeesForKpiAssignment = async () => {
			if (!assignEmployeeSelect) return;
			const res = await api('/api/admin/users');
			if (res.ok && Array.isArray(res.payload)) {
				const employees = res.payload.filter(user => user.active && user.role === 'EMPLOYEE' && user.employeeId);
				assignEmployeeSelect.innerHTML = '<option value="">Select an active employee</option>';
				employees.forEach(emp => {
					const option = document.createElement('option');
					option.value = emp.employeeId;
					option.textContent = `${emp.employeeId} - ${emp.fullName || emp.username || 'Unnamed'}`;
					option.dataset.name = emp.fullName || emp.username || '';
					assignEmployeeSelect.appendChild(option);
				});
				updateSelectedAssignmentEmployeeName();
			}
		};

		if (assignEmployeeSelect) {
			assignEmployeeSelect.addEventListener('change', () => {
				updateSelectedAssignmentEmployeeName();
			});
		}

		// Load KPI categories for assignment
		const loadKPICategoriesForAssignment = async () => {
			if (!assignKpiCategory) return;
			const res = await api('/api/admin/kpis');
			if (res.ok && res.payload) {
				assignKpiCategory.innerHTML = '<option value="">Select a KPI category</option>';
				res.payload
					.filter(kpi => kpi.active !== false)
					.forEach(kpi => {
						const option = document.createElement('option');
						option.value = kpi.id;
						option.textContent = `${kpi.kpiId} - ${kpi.name}`;
						assignKpiCategory.appendChild(option);
					});
			}
		};

		// Load KPI assignments
		const loadKPIAssignments = async (searchTerm = '') => {
			if (!assignmentResultBox) return;
			const res = await api('/api/admin/kpi-assignments');
			if (res.ok && res.payload) {
				let assignments = res.payload;
				// Filter by search term if provided
				if (searchTerm) {
					const term = searchTerm.toLowerCase();
					assignments = assignments.filter(a => 
						(a.employeeId && a.employeeId.toLowerCase().includes(term)) ||
						(a.employeeName && a.employeeName.toLowerCase().includes(term)) ||
						(a.kpiId && a.kpiId.toLowerCase().includes(term)) ||
						(a.kpiName && a.kpiName.toLowerCase().includes(term))
					);
				}
				renderKPIAssignmentTable(assignments);
			} else {
				assignmentResultBox.innerHTML = '<p style="text-align: center; color: #888;">Failed to load KPI assignments.</p>';
			}
		};

		// Render KPI assignments table
		const renderKPIAssignmentTable = (assignments) => {
			if (!assignmentResultBox) return;
			if (!assignments || assignments.length === 0) {
				assignmentResultBox.innerHTML = '<div class="empty-state-modern">No KPI assignments found.</div>';
				return;
			}
			adminAssignmentCache.clear();
			updateDueBanner(assignments);
			const table = `
				<div class="table-responsive">
					<table class="table-modern">
						<thead>
							<tr>
								<th>Employee ID</th>
								<th>Employee Name</th>
								<th>KPI ID</th>
								<th>KPI Name</th>
								<th>Current Progress</th>
								<th>Progress %</th>
								<th>Evidence</th>
								<th>Due Date</th>
								<th style="text-align: right;">Bonus Amount (RM)</th>
								<th>Status</th>
								<th style="text-align: center; width: 170px;">Actions</th>
							</tr>
						</thead>
						<tbody>
							${assignments.map(assignment => {
								adminAssignmentCache.set(assignment.id, assignment);
								const dueDate = assignment.dueDate ? new Date(assignment.dueDate).toLocaleDateString() : 'N/A';
								const bonusAmount = assignment.bonusAmount != null ? parseFloat(assignment.bonusAmount).toFixed(2) : 'N/A';
								const currentValue = assignment.currentProgressValue != null ? assignment.currentProgressValue : 0;
								const targetValue = assignment.measurableValue || '0';
								const progressPercent = assignment.progressPercentage != null ? assignment.progressPercentage.toFixed(2) + '%' : '0%';
								const evidenceLink = assignment.evidenceFilename
									? `<a href="/api/employee/kpis/evidence/${encodeURIComponent(assignment.evidenceFilename)}" target="_blank" style="color: #2f4ceb; text-decoration: none;">View</a>`
									: '<span style="color: #888;">No file</span>';
								const dueReached = isDueDateReached(assignment.dueDate);
								const pendingStatus = isPendingStatus(assignment);
								const rowHighlight = dueReached && pendingStatus ? 'background-color: #fff8e1;' : '';
								const dueBadge = dueReached
									? '<div style="margin-top: 0.25rem;"><span class="status-pill-modern pending">Due date reached</span></div>'
									: '';
								const statusBadge = getAdminStatusBadge(assignment.status);
								const evaluateDisabledAttr = dueReached ? '' : 'disabled title="Evaluation unlocks after the due date"';
								const showEvaluateButton = dueReached && pendingStatus;
								// Escape assignment.id to prevent XSS and ensure it works in onclick
								const safeAssignmentId = (assignment.id || '').replace(/'/g, "\\'");
								return `
								<tr style="${rowHighlight}">
									<td style="font-weight: 500; color: #2f4ceb;">${assignment.employeeId || 'N/A'}</td>
									<td>${assignment.employeeName || 'N/A'}</td>
									<td style="font-weight: 500; color: #2f4ceb;">${assignment.kpiId || 'N/A'}</td>
									<td>${assignment.kpiName || 'N/A'}</td>
									<td>${currentValue} / ${targetValue}</td>
									<td>${progressPercent}</td>
									<td>${evidenceLink}</td>
									<td>${dueDate}${dueBadge}</td>
									<td style="text-align: right; font-weight: 500;">RM ${bonusAmount}</td>
									<td>${statusBadge}</td>
									<td style="text-align: center;">
										<div style="display: flex; flex-direction: column; gap: 8px; align-items: center;">
											${showEvaluateButton ? `
												<button ${evaluateDisabledAttr}
													class="action-btn-modern evaluate-kpi-btn"
													data-assignment-id="${safeAssignmentId}"
													type="button">
													Evaluate
												</button>` : ''}
											<button onclick="window.DashboardApp.unassignKPI('${assignment.id}')" 
												class="action-btn-modern danger">
												Remove
											</button>
										</div>
									</td>
								</tr>
								`;
							}).join('')}
						</tbody>
					</table>
				</div>
			`;
			assignmentResultBox.innerHTML = table;
			
			// Attach event listeners to Evaluate buttons using event delegation
			const evaluateButtons = assignmentResultBox.querySelectorAll('.evaluate-kpi-btn');
			console.log('Found evaluate buttons:', evaluateButtons.length); // Debug log
			evaluateButtons.forEach((button, index) => {
				console.log(`Attaching listener to button ${index}, assignmentId:`, button.getAttribute('data-assignment-id')); // Debug log
				// Remove any existing listeners by cloning
				const newButton = button.cloneNode(true);
				button.parentNode.replaceChild(newButton, button);
				
				newButton.addEventListener('click', async (e) => {
					e.preventDefault();
					e.stopPropagation();
					e.stopImmediatePropagation(); // Prevent other handlers
					
					// Prevent multiple clicks
					if (newButton.disabled || newButton.classList.contains('processing')) {
						console.log('Button already processing, ignoring click');
						return;
					}
					newButton.classList.add('processing');
					
					console.log('Evaluate button clicked!'); // Debug log
					const assignmentId = newButton.getAttribute('data-assignment-id');
					console.log('Assignment ID from button:', assignmentId); // Debug log
					
					if (!assignmentId) {
						console.error('Assignment ID not found on button');
						showStatus('Assignment ID not found. Please refresh the page.');
						newButton.classList.remove('processing');
						return;
					}
					
					console.log('Calling openKpiEvaluationModal with:', assignmentId);
					try {
						// Use window.DashboardApp to ensure function is accessible
						if (window.DashboardApp && window.DashboardApp.openKpiEvaluationModal) {
							await window.DashboardApp.openKpiEvaluationModal(assignmentId);
						} else if (typeof openKpiEvaluationModal === 'function') {
							await openKpiEvaluationModal(assignmentId);
						} else {
							console.error('openKpiEvaluationModal function not found');
							showStatus('Evaluation function not available. Please refresh the page.');
						}
					} catch (error) {
						console.error('Error opening evaluation modal:', error);
						showStatus('Error: ' + error.message);
					} finally {
						// Re-enable button after a short delay
						setTimeout(() => {
							newButton.classList.remove('processing');
						}, 1000);
					}
				});
			});
		};

		window.DashboardApp.refreshKpiAssignments = () => {
			loadKPIAssignments(assignmentSearchInput?.value.trim() || '');
		};

		// Assign KPI form submission
		if (assignForm) {
			assignForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				const formData = new FormData(assignForm);
				const assignmentType = formData.get('assignmentType');
				const kpiCategoryId = formData.get('kpiCategoryId');

				if (!assignmentType || !kpiCategoryId) {
					showStatus('Please fill in all required fields');
					return;
				}

				const data = {
					kpiCategoryId: kpiCategoryId,
					employeeId: assignmentType === 'individual' ? formData.get('employeeId')?.trim() : null,
					departmentName: assignmentType === 'bulk' ? formData.get('departmentName')?.trim() : null
				};

				if (assignmentType === 'individual' && !data.employeeId) {
					showStatus('Please select an employee for individual assignment');
					return;
				}

				if (assignmentType === 'bulk' && !data.departmentName) {
					showStatus('Department name is required for bulk assignment');
					return;
				}

				const res = await api('/api/admin/kpi-assignments', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(data)
				});

				if (res.ok) {
					showStatus('KPI assigned successfully');
					resetAssignmentState();
					loadKPIAssignments(assignmentSearchInput?.value.trim() || '');
				} else {
					const errorMsg = res.error || res.message || res.payload?.message || 'Unknown error';
					showStatus('Failed to assign KPI: ' + errorMsg);
				}
			});
		}

		// Unassign KPI
		window.DashboardApp.unassignKPI = async (id) => {
			if (!confirm('Are you sure you want to remove this KPI assignment?')) {
				return;
			}
			const res = await api(`/api/admin/kpi-assignments/${id}`, {
				method: 'DELETE'
			});
			if (res.ok) {
				showStatus('KPI assignment removed successfully');
				loadKPIAssignments(assignmentSearchInput?.value.trim() || '');
			} else {
				const errorMsg = res.error || res.message || 'Unknown error';
				showStatus('Failed to remove KPI assignment: ' + errorMsg);
			}
		};

		// Search and refresh buttons for assignments
		if (assignmentSearchButton) {
			assignmentSearchButton.addEventListener('click', () => {
				loadKPIAssignments(assignmentSearchInput?.value.trim() || '');
			});
		}

		if (assignmentRefreshButton) {
			assignmentRefreshButton.addEventListener('click', () => {
				if (assignmentSearchInput) assignmentSearchInput.value = '';
				loadKPIAssignments();
			});
		}

		// Initial loads
		loadDepartments();
		loadActiveEmployeesForKpiAssignment();
		loadKPICategoriesForAssignment();
		loadKPIAssignments();
	};

	const initAdminJobPostings = () => {
		const jobPostingForm = document.getElementById('jobPostingForm');
		if (!jobPostingForm) {
			console.error('Job posting form not found!');
			return;
		}
		const jobPostingTableBody = document.getElementById('jobPostingTableBody');
		const jobPostingEmptyState = document.getElementById('jobPostingEmptyState');
		const jobPostingSearchInput = document.getElementById('jobPostingSearchInput');
		const jobPostingSearchButton = document.getElementById('jobPostingSearchButton');
		const jobPostingStatusFilter = document.getElementById('jobPostingStatusFilter');
		const jobPostingRefreshButton = document.getElementById('jobPostingRefreshButton');
		const jobPostingSubmitButton = document.getElementById('jobPostingSubmitButton');
		const jobPostingCancelButton = document.getElementById('jobPostingCancelButton');
		const jobPostingFormTitle = document.getElementById('jobPostingFormTitle');
		const displayJobPostingId = document.getElementById('displayJobPostingId');
		const candidatesSection = document.getElementById('candidatesSection');
		const candidatesTableBody = document.getElementById('candidatesTableBody');
		const candidatesEmptyState = document.getElementById('candidatesEmptyState');
		const addCandidateButton = document.getElementById('addCandidateButton');
		const addCandidateForm = document.getElementById('addCandidateForm');
		const candidateForm = document.getElementById('candidateForm');
		const candidateCancelButton = document.getElementById('candidateCancelButton');
		const resumeIdDisplay = document.getElementById('resumeIdDisplay');
		const displayResumeHiddenInput = document.getElementById('displayResumeId');
		const candidateJobPostingIdInput = document.getElementById('candidateJobPostingId');
		const candidateFormTitle = document.getElementById('candidateFormTitle');
		const candidateRecordIdInput = document.getElementById('candidateRecordId');
		const candidateStatusNotice = document.getElementById('candidateStatusNotice');
		const resumeFileInput = document.getElementById('resumeFileInput');
		const candidateNameInput = document.getElementById('candidateNameInput');
		const candidateEmailInput = document.getElementById('candidateEmailInput');
		const candidateContactInput = document.getElementById('candidateContactInput');
		const selectedJobPostingTitle = document.getElementById('selectedJobPostingTitle');

		let jobPostingCache = [];
		let selectedJobPostingId = null;
		let selectedJobPostingStatus = null;
		let candidateCache = [];
		let isEditingCandidate = false;

		const getCurrentSearchTerm = () => jobPostingSearchInput?.value.trim() || '';
		const getCurrentStatusFilter = () => jobPostingStatusFilter?.value || '';

		const setResumeIdFields = (value) => {
			if (resumeIdDisplay) resumeIdDisplay.value = value || '';
			if (displayResumeHiddenInput) displayResumeHiddenInput.value = value || '';
		};

		const formatContactForInput = (contactNumber) => {
			if (!contactNumber) return '';
			return contactNumber.startsWith('+60') ? contactNumber.substring(3) : contactNumber;
		};

		const updateCandidateActionsAvailability = () => {
			const isHired = selectedJobPostingStatus && selectedJobPostingStatus.toLowerCase() === 'hired';
			if (addCandidateButton) {
				addCandidateButton.disabled = isHired;
				addCandidateButton.style.opacity = isHired ? '0.6' : '';
				addCandidateButton.style.cursor = isHired ? 'not-allowed' : 'pointer';
			}
			if (candidateStatusNotice) {
				if (isHired) {
					candidateStatusNotice.style.display = 'block';
					candidateStatusNotice.textContent = 'This job posting is marked as Hired. Adding candidates is disabled.';
				} else {
					candidateStatusNotice.style.display = 'none';
					candidateStatusNotice.textContent = '';
				}
			}
			if (isHired && addCandidateForm) {
				addCandidateForm.style.display = 'none';
			}
		};

		const clearCandidateForm = () => {
			if (candidateForm) candidateForm.reset();
			if (candidateRecordIdInput) candidateRecordIdInput.value = '';
			if (candidateFormTitle) candidateFormTitle.textContent = 'Add Candidate';
			if (resumeFileInput) {
				resumeFileInput.value = '';
				resumeFileInput.required = true;
			}
			if (candidateJobPostingIdInput && selectedJobPostingId) {
				candidateJobPostingIdInput.value = selectedJobPostingId;
			}
			setResumeIdFields('');
			isEditingCandidate = false;
		};

		const enterCreateCandidateMode = async () => {
			if (!selectedJobPostingId) {
				showStatus('Please select a job posting first');
				return;
			}
			if (selectedJobPostingStatus && selectedJobPostingStatus.toLowerCase() === 'hired') {
				showStatus('Cannot add candidates to a job posting marked as Hired');
				return;
			}
			clearCandidateForm();
			if (candidateJobPostingIdInput) candidateJobPostingIdInput.value = selectedJobPostingId;
			if (addCandidateForm) addCandidateForm.style.display = 'block';
			await loadNextResumeId();
			addCandidateForm.scrollIntoView({ behavior: 'smooth' });
		};

		const enterEditCandidateMode = (candidate) => {
			if (!candidate) return;
			if (candidateForm) candidateForm.reset();
			if (candidateFormTitle) candidateFormTitle.textContent = 'Edit Candidate';
			if (candidateRecordIdInput) candidateRecordIdInput.value = candidate.id;
			if (candidateJobPostingIdInput) candidateJobPostingIdInput.value = candidate.jobPostingId;
			if (candidateNameInput) candidateNameInput.value = candidate.candidateName || '';
			if (candidateEmailInput) candidateEmailInput.value = candidate.candidateEmail || '';
			if (candidateContactInput) candidateContactInput.value = formatContactForInput(candidate.candidateContactNumber);
			setResumeIdFields(candidate.resumeId || '');
			if (resumeFileInput) {
				resumeFileInput.value = '';
				resumeFileInput.required = false;
			}
			if (addCandidateForm) addCandidateForm.style.display = 'block';
			isEditingCandidate = true;
			addCandidateForm.scrollIntoView({ behavior: 'smooth' });
		};

		const downloadResumeFile = (filename, originalName) => {
			if (!filename) return;
			const link = document.createElement('a');
			link.href = `/api/admin/resumes/file/${encodeURIComponent(filename)}?download=true`;
			link.download = originalName || filename;
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
		};

		// Set today's date
		const setTodayDate = () => {
			const createdDateInput = document.getElementById('createdDateInput');
			if (!createdDateInput) return;
			const today = new Date();
			const yyyy = today.getFullYear();
			const mm = String(today.getMonth() + 1).padStart(2, '0');
			const dd = String(today.getDate()).padStart(2, '0');
			const todayString = `${yyyy}-${mm}-${dd}`;
			createdDateInput.value = todayString;
		};

		// Load next job posting ID
		const loadNextJobPostingId = async () => {
			if (!displayJobPostingId) return;
			try {
				// Use fetch directly for plain text response
				const response = await fetch('/api/admin/job-postings/next-job-posting-id', {
					credentials: 'same-origin'
				});
				if (response.ok) {
					const jobPostingId = await response.text();
					if (jobPostingId && displayJobPostingId) {
						displayJobPostingId.value = jobPostingId.trim();
					}
				}
			} catch (error) {
				console.error('Failed to load next job posting ID:', error);
			}
		};

		// Load next resume ID
		const loadNextResumeId = async () => {
			if (!resumeIdDisplay) {
				console.warn('Resume ID display element not found');
				return;
			}
			try {
				// Use fetch directly for plain text response
				const response = await fetch('/api/admin/resumes/next-resume-id', {
					credentials: 'same-origin'
				});
				if (response.ok) {
					const resumeId = await response.text();
					if (resumeId && resumeId.trim()) {
						setResumeIdFields(resumeId.trim());
					} else {
						console.warn('Empty resume ID received from server');
					}
				} else {
					console.error('Failed to fetch next resume ID:', response.status, await response.text());
				}
			} catch (error) {
				console.error('Failed to load next resume ID:', error);
			}
		};

		// Load job postings
		const loadJobPostings = async (searchTerm = '', statusFilter = '') => {
			const params = new URLSearchParams();
			if (searchTerm) params.append('search', searchTerm);
			if (statusFilter) params.append('status', statusFilter);
			const queryString = params.toString();
			const endpoint = queryString ? `/api/admin/job-postings/search?${queryString}` : '/api/admin/job-postings/search';
			const res = await api(endpoint);
			if (res.ok && res.payload) {
				jobPostingCache = res.payload;
				if (selectedJobPostingId) {
					const currentPosting = jobPostingCache.find(p => p.jobPostingId === selectedJobPostingId || p.id === selectedJobPostingId);
					selectedJobPostingStatus = currentPosting?.status || null;
					updateCandidateActionsAvailability();
				}
				renderJobPostingTable(jobPostingCache);
			} else {
				jobPostingCache = [];
				if (jobPostingTableBody) jobPostingTableBody.innerHTML = '';
				if (jobPostingEmptyState) jobPostingEmptyState.style.display = 'block';
				selectedJobPostingStatus = null;
				updateCandidateActionsAvailability();
			}
		};

		// Render job posting table
		const renderJobPostingTable = (postings) => {
			if (!jobPostingTableBody) return;
			if (!postings || postings.length === 0) {
				jobPostingTableBody.innerHTML = '';
				if (jobPostingEmptyState) jobPostingEmptyState.style.display = 'block';
				return;
			}
			if (jobPostingEmptyState) jobPostingEmptyState.style.display = 'none';
			jobPostingTableBody.innerHTML = postings.map(posting => {
				const createdDate = posting.createdDate ? new Date(posting.createdDate).toLocaleDateString() : 'N/A';
				const statusClass = posting.status === 'Available' ? 'available' : 'hired';
				const statusBadge = `<span class="status-pill-modern ${statusClass}">${posting.status || 'N/A'}</span>`;
				const createdByDisplay = posting.createdByEmployeeId && posting.createdByName
					? `${posting.createdByEmployeeId} - ${posting.createdByName}`
					: (posting.createdByEmployeeId || posting.createdByName || posting.createdBy || 'N/A');
				return `
					<tr>
						<td style="font-weight: 500; color: #2f4ceb;">${posting.jobPostingId || 'N/A'}</td>
						<td style="font-weight: 500;">${posting.jobTitle || 'N/A'}</td>
						<td>${createdDate}</td>
						<td>${statusBadge}</td>
						<td>${createdByDisplay}</td>
						<td style="text-align: center;">
							<div style="display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;">
								<button class="edit-job-posting-btn action-btn-modern" data-id="${posting.id}">Edit</button>
								<button class="view-candidates-btn action-btn-modern success" data-id="${posting.id}" data-job-posting-id="${posting.jobPostingId}" data-job-title="${(posting.jobTitle || '').replace(/"/g, '&quot;')}">View Candidates</button>
								<button class="delete-job-posting-btn action-btn-modern danger" data-id="${posting.id}">Delete</button>
							</div>
						</td>
					</tr>
				`;
			}).join('');
		};

		// Edit job posting
		const editJobPosting = async (id) => {
			const posting = jobPostingCache.find(p => p.id === id);
			if (!posting) {
				showStatus('Job posting not found');
				return;
			}
			if (jobPostingForm) {
				document.getElementById('jobPostingMongoId').value = posting.id;
				document.getElementById('jobPostingId').value = posting.jobPostingId;
				document.getElementById('jobTitleInput').value = posting.jobTitle || '';
				document.getElementById('jobDescriptionInput').value = posting.jobDescription || '';
				document.getElementById('createdDateInput').value = posting.createdDate || '';
				document.getElementById('statusInput').value = posting.status || '';
				if (displayJobPostingId) displayJobPostingId.value = posting.jobPostingId || '';
				if (jobPostingFormTitle) jobPostingFormTitle.textContent = 'Edit Job Posting';
				if (jobPostingSubmitButton) jobPostingSubmitButton.textContent = 'Update Job Posting';
				if (jobPostingCancelButton) jobPostingCancelButton.style.display = 'inline-block';
				jobPostingForm.scrollIntoView({ behavior: 'smooth' });
			}
		};

		// Delete job posting
		const deleteJobPosting = async (id) => {
			if (!confirm('Are you sure you want to delete this job posting? All associated candidates will also be deleted.')) {
				return;
			}
			
			// Check if the job posting being deleted is the one currently being viewed
			const postingToDelete = jobPostingCache.find(p => p.id === id);
			const isCurrentlyViewed = postingToDelete && postingToDelete.jobPostingId === selectedJobPostingId;
			
			const res = await api(`/api/admin/job-postings/${id}`, { method: 'DELETE' });
			if (res.ok) {
				showStatus('Job posting deleted successfully');
				
				// If the deleted job posting was the one being viewed, hide the candidates section
				if (isCurrentlyViewed) {
					if (candidatesSection) candidatesSection.style.display = 'none';
					selectedJobPostingId = null;
					selectedJobPostingStatus = null;
					if (addCandidateForm) addCandidateForm.style.display = 'none';
					clearCandidateForm();
					updateCandidateActionsAvailability();
				}
				
				loadJobPostings(getCurrentSearchTerm(), getCurrentStatusFilter());
				// Refresh the next job posting ID after deletion
				loadNextJobPostingId();
			} else {
				showStatus(res.error || res.message || 'Failed to delete job posting');
			}
		};

		// View candidates
		const viewCandidates = async (id, jobPostingId, jobTitle) => {
			selectedJobPostingId = jobPostingId;
			const postingRecord = jobPostingCache.find(p => p.jobPostingId === jobPostingId || p.id === id);
			selectedJobPostingStatus = postingRecord?.status || null;
			if (selectedJobPostingTitle) selectedJobPostingTitle.textContent = jobTitle || 'N/A';
			if (candidatesSection) candidatesSection.style.display = 'block';
			if (candidateJobPostingIdInput) candidateJobPostingIdInput.value = jobPostingId;
			clearCandidateForm();
			updateCandidateActionsAvailability();
			loadCandidates(jobPostingId);
			candidatesSection.scrollIntoView({ behavior: 'smooth' });
		};

		// Load candidates
		const loadCandidates = async (jobPostingId) => {
			const res = await api(`/api/admin/resumes/job-posting/${encodeURIComponent(jobPostingId)}`);
			if (res.ok && res.payload) {
				candidateCache = res.payload;
				renderCandidatesTable(candidateCache);
			} else {
				candidateCache = [];
				if (candidatesTableBody) candidatesTableBody.innerHTML = '';
				if (candidatesEmptyState) candidatesEmptyState.style.display = 'block';
			}
		};

		// Render candidates table
		const renderCandidatesTable = (candidates) => {
			if (!candidatesTableBody) return;
			if (!candidates || candidates.length === 0) {
				candidatesTableBody.innerHTML = '';
				if (candidatesEmptyState) candidatesEmptyState.style.display = 'block';
				return;
			}
			if (candidatesEmptyState) candidatesEmptyState.style.display = 'none';
			candidatesTableBody.innerHTML = candidates.map(candidate => {
				const uploadDate = candidate.uploadDate ? new Date(candidate.uploadDate).toLocaleDateString() : 'N/A';
				const resumeActions = candidate.resumeFilename
					? `<div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-start;">
							<a href="/api/admin/resumes/file/${encodeURIComponent(candidate.resumeFilename)}" target="_blank" style="color: #2f4ceb; text-decoration: none;">View</a>
							<button type="button" class="download-resume-btn action-btn-modern" data-filename="${candidate.resumeFilename}" data-original-name="${candidate.resumeOriginalName || ''}">Download</button>
					   </div>`
					: '<span style="color: #888;">No file</span>';
				return `
					<tr>
						<td style="font-weight: 500; color: #2f4ceb;">${candidate.resumeId || 'N/A'}</td>
						<td style="font-weight: 500;">${candidate.candidateName || 'N/A'}</td>
						<td>${candidate.candidateEmail || 'N/A'}</td>
						<td>${candidate.candidateContactNumber || 'N/A'}</td>
						<td>${uploadDate}</td>
						<td>${resumeActions}</td>
						<td style="text-align: center;">
							<div style="display: flex; gap: 8px; justify-content: center;">
								<button class="edit-candidate-btn action-btn-modern" data-id="${candidate.id}">Edit</button>
								<button class="delete-resume-btn action-btn-modern danger" data-id="${candidate.id}" data-resume-id="${candidate.resumeId}">Delete</button>
							</div>
						</td>
					</tr>
				`;
			}).join('');
		};

		// Delete resume
		const deleteResume = async (id, resumeId) => {
			if (!confirm('Are you sure you want to delete this candidate record?')) {
				return;
			}
			const res = await api(`/api/admin/resumes/${id}`, { method: 'DELETE' });
			if (res.ok) {
				showStatus('Candidate record deleted successfully');
				if (candidateRecordIdInput && candidateRecordIdInput.value === id) {
					clearCandidateForm();
					if (addCandidateForm) addCandidateForm.style.display = 'none';
				}
				if (selectedJobPostingId) loadCandidates(selectedJobPostingId);
				// Refresh the next resume ID after deletion
				loadNextResumeId();
			} else {
				showStatus(res.error || res.message || 'Failed to delete candidate record');
			}
		};

		// Add candidate button
		if (addCandidateButton) {
			addCandidateButton.addEventListener('click', async () => {
				enterCreateCandidateMode();	
			});
		}

		// Cancel candidate form
		if (candidateCancelButton) {
			candidateCancelButton.addEventListener('click', () => {
				if (addCandidateForm) addCandidateForm.style.display = 'none';
				clearCandidateForm();
			});
		}

		// Candidate form submission
		if (candidateForm) {
			candidateForm.addEventListener('submit', async (event) => {
				event.preventDefault();
				const jobPostingId = candidateJobPostingIdInput?.value;
				const candidateName = (candidateNameInput?.value || '').trim();
				const candidateEmail = (candidateEmailInput?.value || '').trim();
				const candidateContact = (candidateContactInput?.value || '').trim();
				const resumeFile = resumeFileInput?.files[0];
				const candidateRecordId = candidateRecordIdInput?.value;
				const isEditing = Boolean(candidateRecordId);

				if (!jobPostingId || !candidateName || !candidateEmail || !candidateContact) {
					showStatus('Please fill in all required fields');
					return;
				}
				if (!isEditing && !resumeFile) {
					showStatus('Resume file is required when adding a candidate');
					return;
				}

				// Validate candidate email (same rules as employee creation)
				if (candidateEmail.includes(' ')) {
					showStatus('Candidate email must not contain spaces');
					return;
				}
				if (!candidateEmail.includes('@')) {
					showStatus('Candidate email must contain @');
					return;
				}
				const atIndex = candidateEmail.indexOf('@');
				const domainPart = candidateEmail.substring(atIndex + 1);
				if (!domainPart.includes('.')) {
					showStatus('Candidate email must contain . after @');
					return;
				}

				// Validate candidate contact number (same rules as employee creation)
				const cleanedContact = candidateContact.replace(/[\s\-]/g, '');
				if (!/^[0-9]{9,10}$/.test(cleanedContact)) {
					showStatus('Candidate contact number must be 9-10 digits');
					return;
				}
				const normalizedContact = `+60${cleanedContact}`;

				const formData = new FormData();
				formData.append('jobPostingId', jobPostingId);
				formData.append('candidateName', candidateName);
				formData.append('candidateEmail', candidateEmail);
				formData.append('candidateContactNumber', normalizedContact);
				if (resumeFile) {
					formData.append('resumeFile', resumeFile);
				}

				try {
					const response = await fetch(isEditing ? `/api/admin/resumes/${candidateRecordId}` : '/api/admin/resumes', {
						method: isEditing ? 'PUT' : 'POST',
						body: formData
					});

					if (response.ok) {
						showStatus(isEditing ? 'Candidate updated successfully' : 'Candidate added successfully');
						if (addCandidateForm) addCandidateForm.style.display = 'none';
						clearCandidateForm();
						if (!isEditing) {
							await loadNextResumeId();
						}
						if (selectedJobPostingId) loadCandidates(selectedJobPostingId);
					} else {
						const text = await response.text();
						let errorMessage = isEditing ? 'Failed to update candidate' : 'Failed to add candidate';
						try {
							const parsed = JSON.parse(text);
							errorMessage = parsed.message || errorMessage;
						} catch (_) {
							if (text && text.trim()) {
								errorMessage = text.trim();
							}
						}
						showStatus(errorMessage);
					}
				} catch (error) {
					showStatus(error.message || 'Failed to save candidate');
				}
			});
		}

		// Job posting form submission handler
		const handleJobPostingSubmit = async (event) => {
			event.preventDefault();
			event.stopPropagation();
			
			// Show loading status
			showStatus('Creating job posting...');
			
			try {
				// Ensure date is set if it's empty
				const createdDateInput = document.getElementById('createdDateInput');
				if (createdDateInput && !createdDateInput.value) {
					setTodayDate();
				}

				const formData = new FormData(jobPostingForm);
				const id = formData.get('id');
				const jobTitle = formData.get('jobTitle')?.trim();
				const jobDescription = formData.get('jobDescription')?.trim() || '';
				let createdDate = formData.get('createdDate');
				
				// If date is still empty, set it to today
				if (!createdDate && createdDateInput) {
					const today = new Date();
					const yyyy = today.getFullYear();
					const mm = String(today.getMonth() + 1).padStart(2, '0');
					const dd = String(today.getDate()).padStart(2, '0');
					createdDate = `${yyyy}-${mm}-${dd}`;
					createdDateInput.value = createdDate;
				}
				
				const status = formData.get('status');

				if (!jobTitle || !createdDate || !status) {
					showStatus('Please fill in all required fields. Missing: ' + 
						(!jobTitle ? 'Job Title' : '') + 
						(!createdDate ? ' Created Date' : '') + 
						(!status ? ' Status' : ''));
					return;
				}

				if (!session.userId) {
					showStatus('Session expired. Please refresh the page.');
					return;
				}

				const data = {
					jobTitle,
					jobDescription,
					createdDate,
					status
				};

				let res;
				if (id) {
					// Update
					res = await api(`/api/admin/job-postings/${id}`, {
						method: 'PUT',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify(data)
					});
				} else {
					// Create
					const url = `/api/admin/job-postings?createdBy=${encodeURIComponent(session.userId)}`;
					res = await api(url, {
						method: 'POST',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify(data)
					});
				}

				// Check response status - Spring returns 200 for successful POST
				// res.ok is true when status is 200-299
				if (res.ok) {
					showStatus(id ? 'Job posting updated successfully' : 'Job posting created successfully');
					jobPostingForm.reset();
					if (jobPostingFormTitle) jobPostingFormTitle.textContent = 'Create New Job Posting';
					if (jobPostingSubmitButton) jobPostingSubmitButton.textContent = 'Create Job Posting';
					if (jobPostingCancelButton) jobPostingCancelButton.style.display = 'none';
					setTodayDate();
					loadNextJobPostingId();
					loadJobPostings(getCurrentSearchTerm(), getCurrentStatusFilter());
				} else {
					// Extract error message
					let errorMsg = 'Failed to save job posting';
					
					// Check rawText first (most reliable)
					if (res.rawText) {
						try {
							const parsed = JSON.parse(res.rawText);
							if (parsed.message) {
								errorMsg = parsed.message;
							} else if (parsed.error) {
								errorMsg = parsed.error;
							} else if (Array.isArray(parsed) && parsed.length > 0) {
								// Handle validation errors array
								errorMsg = parsed.map(e => e.defaultMessage || e.message || JSON.stringify(e)).join(', ');
							}
						} catch {
							if (res.rawText.trim()) {
								errorMsg = res.rawText.trim();
							}
						}
					}
					
					// Check payload as fallback
					if (res.payload && errorMsg === 'Failed to save job posting') {
						if (typeof res.payload === 'object') {
							if (res.payload.message) {
								errorMsg = res.payload.message;
							} else if (res.payload.error) {
								errorMsg = res.payload.error;
							} else if (Array.isArray(res.payload)) {
								errorMsg = res.payload.map(e => e.defaultMessage || e.message || JSON.stringify(e)).join(', ');
							}
						} else if (typeof res.payload === 'string') {
							errorMsg = res.payload;
						}
					}
					
					// Show error message
					showStatus('Error (Status ' + res.status + '): ' + errorMsg);
				}
			} catch (error) {
				console.error('Error submitting job posting form:', error);
				showStatus('An error occurred: ' + (error.message || 'Unknown error'));
			}
		};

		// Attach submit event listener to form
		jobPostingForm.addEventListener('submit', handleJobPostingSubmit);

		// Cancel edit
		if (jobPostingCancelButton) {
			jobPostingCancelButton.addEventListener('click', () => {
				if (jobPostingForm) jobPostingForm.reset();
				if (jobPostingFormTitle) jobPostingFormTitle.textContent = 'Create New Job Posting';
				if (jobPostingSubmitButton) jobPostingSubmitButton.textContent = 'Create Job Posting';
				if (jobPostingCancelButton) jobPostingCancelButton.style.display = 'none';
				setTodayDate();
				loadNextJobPostingId();
			});
		}

		// Search and refresh
		if (jobPostingSearchButton) {
			jobPostingSearchButton.addEventListener('click', () => {
				loadJobPostings(getCurrentSearchTerm(), getCurrentStatusFilter());
			});
		}

		if (jobPostingStatusFilter) {
			jobPostingStatusFilter.addEventListener('change', () => {
				loadJobPostings(getCurrentSearchTerm(), getCurrentStatusFilter());
			});
		}

		if (jobPostingRefreshButton) {
			jobPostingRefreshButton.addEventListener('click', () => {
				if (jobPostingSearchInput) jobPostingSearchInput.value = '';
				if (jobPostingStatusFilter) jobPostingStatusFilter.value = '';
				loadJobPostings();
			});
		}

		// Event delegation for job posting buttons
		if (jobPostingTableBody) {
			jobPostingTableBody.addEventListener('click', (e) => {
				if (e.target.classList.contains('edit-job-posting-btn')) {
					const id = e.target.getAttribute('data-id');
					if (id) editJobPosting(id);
				} else if (e.target.classList.contains('delete-job-posting-btn')) {
					const id = e.target.getAttribute('data-id');
					if (id) deleteJobPosting(id);
				} else if (e.target.classList.contains('view-candidates-btn')) {
					const id = e.target.getAttribute('data-id');
					const jobPostingId = e.target.getAttribute('data-job-posting-id');
					const jobTitle = e.target.getAttribute('data-job-title');
					if (id && jobPostingId) viewCandidates(id, jobPostingId, jobTitle);
				}
			});
		}

		// Event delegation for candidate/resume buttons
		if (candidatesTableBody) {
			candidatesTableBody.addEventListener('click', (e) => {
				const targetButton = e.target.closest('button');
				if (targetButton && targetButton.classList.contains('delete-resume-btn')) {
					const id = targetButton.getAttribute('data-id');
					const resumeId = targetButton.getAttribute('data-resume-id');
					if (id && resumeId) deleteResume(id, resumeId);
				} else if (targetButton && targetButton.classList.contains('edit-candidate-btn')) {
					const id = targetButton.getAttribute('data-id');
					const candidate = candidateCache.find(c => c.id === id);
					if (candidate) {
						enterEditCandidateMode(candidate);
					} else {
						showStatus('Candidate data not found for editing');
					}
				} else if (targetButton && targetButton.classList.contains('download-resume-btn')) {
					const filename = targetButton.getAttribute('data-filename');
					const originalName = targetButton.getAttribute('data-original-name');
					downloadResumeFile(filename, originalName);
				}
			});
		}

		// Initial loads
		// Set date immediately (synchronous)
		setTodayDate();
		// Load job posting ID and list (asynchronous)
		loadNextJobPostingId();
		loadJobPostings();
	};

	return {
		session,
		api,
		showStatus,
		initSidebar,
		renderUserBadge,
		attachLogout,
		prefillForms,
		initEmployeeProfile,
		initEmployeeBenefits,
		initEmployeeKPIs,
		initEmployeeAttendance,
		initEmployeeLeave,
		initEmployeeOvertime,
		initEmployeePayroll,
		initEmployeeAnnouncements,
		initEmployeeReports,
		initFloatingChatbot,
		initEmployeeChatbot,
		initAdminAttendance,
		initAdminLeave,
		fillLeaveDecisionForm,
		initAdminOvertime,
		fillOvertimeDecisionForm,
		initAdminPayroll,
		initAdminBenefits,
		initAdminKPIs,
		initAdminAnnouncements,
		initAdminUsers,
		initAdminCompanySettings,
		initAdminJobPostings,
		guardRole,
		initInactivityMonitor
	};
})();

// Expose DashboardApp to window for onclick handlers
if (typeof window !== 'undefined') {
	window.DashboardApp = DashboardApp;
}

