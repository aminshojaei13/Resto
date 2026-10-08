<?php

namespace App\Notifications;

use App\Models\StaffInvitation;
use App\Support\RolePermission;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

/**
 * Staff invitation email.
 *
 * The link contains the raw, single-use token. Only its hash is stored.
 */
class StaffInvitationNotification extends Notification
{
    use Queueable;

    public function __construct(
        public string $rawToken,
        public string $organizationName,
        public string $inviterName,
        public string $memberRole = 'STAFF',
        public string $userLocale = 'fa',
    ) {
        $this->userLocale = in_array($userLocale, ['fa', 'en'], true) ? $userLocale : 'fa';
    }

    public function via(object $notifiable): array
    {
        return ['mail'];
    }

    public function toMail(object $notifiable): MailMessage
    {
        $url = rtrim((string) config('resto.frontend_url'), '/')
            . '/accept-invitation?token=' . $this->rawToken;

        $role = RolePermission::canonical($this->memberRole);

        if ($this->userLocale === 'en') {
            return (new MailMessage)
                ->subject('You have been invited to join ' . $this->organizationName)
                ->line($this->inviterName . ' invited you to join ' . $this->organizationName . ' on Resto.')
                ->line('You will be able to ' . $this->roleSummaryEn($role) . '.')
                ->line('This invitation can be used once and expires in ' . StaffInvitation::VALID_FOR_HOURS . ' hours.')
                ->action('Accept invitation', $url)
                ->line('If you were not expecting this invitation you can ignore this email.');
        }

        return (new MailMessage)
            ->subject('دعوت به پیوستن به ' . $this->organizationName)
            ->line($this->inviterName . ' شما را به ' . $this->organizationName . ' در رستو دعوت کرده است.')
            ->line('شما می‌توانید ' . $this->roleSummaryFa($role) . '.')
            ->line('این دعوت فقط یک‌بار قابل استفاده است و پس از '
                . StaffInvitation::VALID_FOR_HOURS . ' ساعت منقضی می‌شود.')
            ->action('پذیرش دعوت', $url)
            ->line('اگر این دعوت را انتظار نداشتید، می‌توانید این ایمیل را نادیده بگیرید.');
    }

    private function roleSummaryFa(string $role): string
    {
        return match ($role) {
            'OWNER' => 'مدیر کسب‌وکار باشید و به همه بخش‌ها دسترسی داشته باشید',
            'MANAGER' => 'امور روزانه کسب‌وکار، کالا و فروش را مدیریت کنید',
            default => 'فروش، مشتریان و مشاهده موجودی را انجام دهید',
        };
    }

    private function roleSummaryEn(string $role): string
    {
        return match ($role) {
            'OWNER' => 'manage this business and have full access',
            'MANAGER' => 'run day-to-day operations, catalog and sales',
            default => 'handle sales, customers and view inventory',
        };
    }
}
