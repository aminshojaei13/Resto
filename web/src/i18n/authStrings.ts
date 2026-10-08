/**
 * Every user-facing string for sign-in, recovery, profile and staff access.
 *
 * Persian and English are kept strictly apart: a Persian screen never shows an
 * English label and vice versa. No technical implementation terms (token,
 * API, tenant, middleware, …) appear here.
 */

export type Language = 'fa' | 'en';

export interface AuthStrings {
  brandName: string;
  brandTagline: string;

  loginTitle: string;
  loginSubtitle: string;
  email: string;
  emailPlaceholder: string;
  password: string;
  passwordPlaceholder: string;
  signIn: string;
  signingIn: string;
  forgotPassword: string;
  showPassword: string;
  hidePassword: string;
  signingInAs: string;

  forgotTitle: string;
  forgotSubtitle: string;
  sendResetLink: string;
  sending: string;
  forgotSentTitle: string;
  forgotSentBody: string;
  forgotSentNote: string;
  backToSignIn: string;

  resetTitle: string;
  resetSubtitle: string;
  resetToken: string;
  newPassword: string;
  confirmPassword: string;
  setNewPassword: string;
  updating: string;
  passwordsDoNotMatch: string;
  resetDoneTitle: string;
  resetDoneBody: string;
  goToSignIn: string;
  resetLinkMissing: string;

  changeTitle: string;
  changeSubtitle: string;
  currentPassword: string;
  newPasswordHint: string;
  changePassword: string;
  changing: string;
  changeDone: string;
  signOutEverywhere: string;
  signOutEverywhereDone: string;

  profileTitle: string;
  profileSubtitle: string;
  firstName: string;
  lastName: string;
  phone: string;
  saveProfile: string;
  saving: string;
  profileSaved: string;
  yourBusinesses: string;
  roleOwner: string;
  roleManager: string;
  roleStaff: string;
  accountStatus: string;
  statusActive: string;
  statusInactive: string;

  staffTitle: string;
  staffSubtitle: string;
  inviteStaff: string;
  inviteTitle: string;
  inviteBody: string;
  inviteEmail: string;
  inviteRole: string;
  sendInvitation: string;
  sending2: string;
  members: string;
  pendingInvites: string;
  noMembers: string;
  resend: string;
  revoke: string;
  deactivate: string;
  reactivate: string;
  remove: string;
  invitedOn: string;
  expiresOn: string;
  staffInviteSent: string;
  confirmRemoveStaff: string;
  confirmDeactivateStaff: string;
  cannotManageStaff: string;

  acceptTitle: string;
  acceptBody: string;
  acceptFor: string;
  acceptInvalid: string;
  acceptDone: string;
  acceptExpiryNote: string;

  sessionExpired: string;
  tooManyAttempts: string;
  offline: string;
  genericError: string;
  required: string;
  invalidEmail: string;
}

const fa: AuthStrings = {
  brandName: 'رستو',
  brandTagline: 'مدیریت کسب‌وکار رستوران و فروشگاه',

  loginTitle: 'ورود به حساب کاربری',
  loginSubtitle: 'برای ادامه، ایمیل و رمز عبور خود را وارد کنید.',
  email: 'ایمیل',
  emailPlaceholder: 'name@example.com',
  password: 'رمز عبور',
  passwordPlaceholder: 'رمز عبور خود را وارد کنید',
  signIn: 'ورود',
  signingIn: 'در حال ورود…',
  forgotPassword: 'فراموشی رمز عبور',
  showPassword: 'نمایش رمز عبور',
  hidePassword: 'پنهان‌کردن رمز عبور',
  signingInAs: 'در حال ورود به عنوان',

  forgotTitle: 'بازیابی رمز عبور',
  forgotSubtitle: 'ایمیل خود را وارد کنید تا لینک بازیابی برای شما ارسال شود.',
  sendResetLink: 'ارسال لینک بازیابی',
  sending: 'در حال ارسال…',
  forgotSentTitle: 'درخواست شما ثبت شد',
  forgotSentBody: 'اگر این ایمیل در سامانه ثبت شده باشد، لینک بازیابی رمز عبور برای آن ارسال می‌شود.',
  forgotSentNote: 'این پیام به‌منظور جلوگیری از افشای اطلاعات، همیشه یکسان نمایش داده می‌شود.',
  backToSignIn: 'بازگشت به صفحه ورود',

  resetTitle: 'تعیین رمز عبور جدید',
  resetSubtitle: 'برای ادامه، رمز عبور تازه‌ای انتخاب کنید.',
  resetToken: 'کد بازیابی',
  newPassword: 'رمز عبور جدید',
  confirmPassword: 'تکرار رمز عبور جدید',
  setNewPassword: 'ثبت رمز عبور جدید',
  updating: 'در حال ثبت…',
  passwordsDoNotMatch: 'رمز عبور و تکرار آن یکسان نیستند.',
  resetDoneTitle: 'رمز عبور تغییر کرد',
  resetDoneBody: 'اکنون می‌توانید با رمز عبور جدید وارد شوید.',
  goToSignIn: 'رفتن به صفحه ورود',
  resetLinkMissing: 'لینک بازیابی نامعتبر یا منقضی شده است. لطفاً دوباره درخواست بازیابی بدهید.',

  changeTitle: 'تغییر رمز عبور',
  changeSubtitle: 'برای ادامه، رمز عبور فعلی خود را وارد کنید.',
  currentPassword: 'رمز عبور فعلی',
  newPasswordHint: 'رمز عبور باید دست‌کم ۱۰ نویسه باشد و نباید رمزی رایج باشد.',
  changePassword: 'ثبت رمز عبور جدید',
  changing: 'در حال ثبت…',
  changeDone: 'رمز عبور شما با موفقیت تغییر کرد.',
  signOutEverywhere: 'خروج از همه دستگاه‌ها',
  signOutEverywhereDone: 'از همه دستگاه‌ها خارج شدید.',

  profileTitle: 'حساب کاربری من',
  profileSubtitle: 'اطلاعاتی که دیگران در این کسب‌وکار می‌بینند.',
  firstName: 'نام',
  lastName: 'نام خانوادگی',
  phone: 'شماره تماس',
  saveProfile: 'ذخیره تغییرات',
  saving: 'در حال ذخیره…',
  profileSaved: 'اطلاعات حساب شما به‌روزرسانی شد.',
  yourBusinesses: 'کسب‌وکارهای شما',
  roleOwner: 'مالک',
  roleManager: 'مدیر',
  roleStaff: 'کارمند',
  accountStatus: 'وضعیت حساب',
  statusActive: 'فعال',
  statusInactive: 'غیرفعال',

  staffTitle: 'کارمندان',
  staffSubtitle: 'اعضای کسب‌وکار و دسترسی هر یک.',
  inviteStaff: 'دعوت از کارمند جدید',
  inviteTitle: 'دعوت کارمند جدید',
  inviteBody: 'با ایمیلی که می‌فرستید، کارمند لینکی دریافت می‌کند و خودش رمز عبورش را می‌سازد.',
  inviteEmail: 'ایمیل کارمند',
  inviteRole: 'نقش',
  sendInvitation: 'ارسال دعوت‌نامه',
  sending2: 'در حال ارسال…',
  members: 'اعضا',
  pendingInvites: 'دعوت‌های در انتظار',
  noMembers: 'هنوز کارمندی اضافه نشده است.',
  resend: 'ارسال دوباره',
  revoke: 'لغو دعوت',
  deactivate: 'غیرفعال‌سازی دسترسی',
  reactivate: 'فعال‌سازی دسترسی',
  remove: 'حذف دسترسی',
  invitedOn: 'دعوت‌شده در',
  expiresOn: 'اعتبار تا',
  staffInviteSent: 'دعوت‌نامه ارسال شد.',
  confirmRemoveStaff: 'دسترسی این شخص به کسب‌وکار حذف شود؟',
  confirmDeactivateStaff: 'دسترسی این شخص غیرفعال شود؟ او بلافاصله از سامانه خارج می‌شود.',
  cannotManageStaff: 'شما اجازه مدیریت کارمندان را ندارید.',

  acceptTitle: 'پذیرش دعوت',
  acceptBody: 'برای پیوستن به کسب‌وکار، رمز عبور خود را انتخاب کنید.',
  acceptFor: 'دعوت‌شده به نام',
  acceptInvalid: 'این دعوت‌نامه نامعتبر یا منقضی شده است. لطفاً از مدیر کسب‌وکار بخواهید دعوت تازه‌ای بفرستد.',
  acceptDone: 'حساب کاربری شما ساخته شد. اکنون می‌توانید وارد شوید.',
  acceptExpiryNote: 'این دعوت فقط یک‌بار و تا زمان انقضا قابل استفاده است.',

  sessionExpired: 'نشست شما پایان یافته است. لطفاً دوباره وارد شوید.',
  tooManyAttempts: 'تلاش‌های ناموفق زیاد بوده است. لطفاً کمی بعد دوباره تلاش کنید.',
  offline: 'ارتباط با سرور برقرار نشد. اتصال اینترنت خود را بررسی کنید.',
  genericError: 'خطایی رخ داد. لطفاً دوباره تلاش کنید.',
  required: 'این فیلد الزامی است.',
  invalidEmail: 'نشانی ایمیل معتبر نیست.',
};

const en: AuthStrings = {
  brandName: 'Resto',
  brandTagline: 'Restaurant and retail business management',

  loginTitle: 'Sign in',
  loginSubtitle: 'Enter your email and password to continue.',
  email: 'Email',
  emailPlaceholder: 'name@example.com',
  password: 'Password',
  passwordPlaceholder: 'Enter your password',
  signIn: 'Sign in',
  signingIn: 'Signing in…',
  forgotPassword: 'Forgot password',
  showPassword: 'Show password',
  hidePassword: 'Hide password',
  signingInAs: 'Signing in as',

  forgotTitle: 'Reset your password',
  forgotSubtitle: 'Enter your email and we will send you a reset link.',
  sendResetLink: 'Send reset link',
  sending: 'Sending…',
  forgotSentTitle: 'Request received',
  forgotSentBody: 'If that email is registered, a password reset link has been sent to it.',
  forgotSentNote: 'This message is always the same, so that no one can find out which addresses exist.',
  backToSignIn: 'Back to sign in',

  resetTitle: 'Choose a new password',
  resetSubtitle: 'Pick a new password to continue.',
  resetToken: 'Reset code',
  newPassword: 'New password',
  confirmPassword: 'Confirm new password',
  setNewPassword: 'Save new password',
  updating: 'Saving…',
  passwordsDoNotMatch: 'The two passwords do not match.',
  resetDoneTitle: 'Password changed',
  resetDoneBody: 'You can now sign in with your new password.',
  goToSignIn: 'Go to sign in',
  resetLinkMissing: 'This reset link is invalid or has expired. Please request a new one.',

  changeTitle: 'Change password',
  changeSubtitle: 'Enter your current password to continue.',
  currentPassword: 'Current password',
  newPasswordHint: 'Use at least 10 characters and avoid common passwords.',
  changePassword: 'Save new password',
  changing: 'Saving…',
  changeDone: 'Your password has been changed.',
  signOutEverywhere: 'Sign out on all devices',
  signOutEverywhereDone: 'You have been signed out on all devices.',

  profileTitle: 'My account',
  profileSubtitle: 'What other people in this business see about you.',
  firstName: 'First name',
  lastName: 'Last name',
  phone: 'Phone',
  saveProfile: 'Save changes',
  saving: 'Saving…',
  profileSaved: 'Your account details have been updated.',
  yourBusinesses: 'Your businesses',
  roleOwner: 'Owner',
  roleManager: 'Manager',
  roleStaff: 'Staff',
  accountStatus: 'Account status',
  statusActive: 'Active',
  statusInactive: 'Inactive',

  staffTitle: 'Staff',
  staffSubtitle: 'People in this business and what each of them can do.',
  inviteStaff: 'Invite a staff member',
  inviteTitle: 'Invite a staff member',
  inviteBody: 'They receive an email with a link, and choose their own password.',
  inviteEmail: 'Staff email',
  inviteRole: 'Role',
  sendInvitation: 'Send invitation',
  sending2: 'Sending…',
  members: 'Members',
  pendingInvites: 'Pending invitations',
  noMembers: 'No staff members yet.',
  resend: 'Send again',
  revoke: 'Cancel invitation',
  deactivate: 'Remove access',
  reactivate: 'Restore access',
  remove: 'Remove access',
  invitedOn: 'Invited',
  expiresOn: 'Valid until',
  staffInviteSent: 'The invitation has been sent.',
  confirmRemoveStaff: 'Remove this person’s access to the business?',
  confirmDeactivateStaff: 'Remove this person’s access? They will be signed out immediately.',
  cannotManageStaff: 'You are not allowed to manage staff.',

  acceptTitle: 'Accept invitation',
  acceptBody: 'Choose a password to join the business.',
  acceptFor: 'Invited as',
  acceptInvalid: 'This invitation is invalid or has expired. Please ask your manager to send a new one.',
  acceptDone: 'Your account has been created. You can now sign in.',
  acceptExpiryNote: 'This invitation can be used once, until it expires.',

  sessionExpired: 'Your session has ended. Please sign in again.',
  tooManyAttempts: 'Too many failed attempts. Please try again in a moment.',
  offline: 'Could not reach the server. Check your connection.',
  genericError: 'Something went wrong. Please try again.',
  required: 'This field is required.',
  invalidEmail: 'That email address is not valid.',
};

export const authStrings: Record<Language, AuthStrings> = { fa, en };

export const t = (language: Language): AuthStrings => authStrings[language] ?? fa;

export const roleLabel = (language: Language, role: string | null | undefined): string => {
  const strings = t(language);

  switch ((role ?? '').toUpperCase()) {
    case 'OWNER':
      return strings.roleOwner;
    case 'MANAGER':
      return strings.roleManager;
    default:
      return strings.roleStaff;
  }
};

export const isPersian = (language: Language): boolean => language === 'fa';
