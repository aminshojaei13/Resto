<?php

namespace App\Notifications;

use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

/**
 * Password recovery email.
 *
 * Content is fully localized: a Persian user receives a Persian email, an
 * English user an English one. No mixed-language strings are ever sent.
 */
class ResetPasswordNotification extends Notification
{
    use Queueable;

    public function __construct(
        public string $resetToken,
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
        $url = $this->resetUrl($notifiable);
        $minutes = (int) config('resto.password_reset.expire_minutes', 60);

        if ($this->userLocale === 'en') {
            return (new MailMessage)
                ->subject('Reset your Resto password')
                ->line('We received a request to reset the password for your Resto account.')
                ->line('This link can be used once and expires in ' . $minutes . ' minutes.')
                ->action('Reset password', $url)
                ->line('If you did not request this, you can safely ignore this email. Your current password stays active until the link is used.');
        }

        return (new MailMessage)
            ->subject('بازیابی رمز عبور رستو')
            ->line('درخواست بازیابی رمز عبور حساب کاربری شما در رستو ثبت شد.')
            ->line('این لینک تنها یک‌بار قابل استفاده است و پس از ' . $minutes . ' دقیقه منقضی می‌شود.')
            ->action('بازیابی رمز عبور', $url)
            ->line('اگر شما این درخواست را ثبت نکرده‌اید، این ایمیل را نادیده بگیرید. تا زمان استفاده از این لینک، رمز عبور فعلی شما همچنان فعال است.');
    }

    private function resetUrl(object $notifiable): string
    {
        return rtrim((string) config('resto.frontend_url'), '/')
            . '/reset-password?token=' . $this->resetToken
            . '&email=' . urlencode((string) $notifiable->getEmailForPasswordReset());
    }
}
