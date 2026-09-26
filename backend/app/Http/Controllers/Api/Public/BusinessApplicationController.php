<?php

namespace App\Http\Controllers\Api\Public;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\BusinessApplication;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class BusinessApplicationController extends Controller
{
    public function store(Request $request)
    {
        $request->validate([
            'business_name' => 'required|string|max:255',
            'owner_name' => 'required|string|max:255',
            'email' => 'required|email|max:255',
            'phone' => 'nullable|string|max:50',
            'business_type' => 'nullable|string|max:50',
            'country' => 'nullable|string|max:100',
            'city' => 'nullable|string|max:100',
            'address' => 'nullable|string',
            'notes' => 'nullable|string',
        ]);

        // Duplicate PENDING application protection
        $existing = BusinessApplication::where('email', $request->email)
            ->where('status', 'PENDING')
            ->first();

        if ($existing) {
            return response()->json([
                'message' => 'Your application has already been submitted and is pending review.',
                'application_id' => $existing->id,
                'status' => 'PENDING',
            ], 200);
        }

        $application = BusinessApplication::create([
            'id' => (string) Str::uuid(),
            'business_name' => $request->business_name,
            'owner_name' => $request->owner_name,
            'email' => strtolower(trim($request->email)),
            'phone' => $request->phone,
            'business_type' => $request->business_type ?? 'RETAIL',
            'country' => $request->country ?? 'IR',
            'city' => $request->city,
            'address' => $request->address,
            'notes' => $request->notes,
            'status' => 'PENDING',
        ]);

        // Audit Log
        AuditLog::create([
            'id' => (string) Str::uuid(),
            'organization_id' => 'system',
            'user_id' => null,
            'action' => 'business.application.created',
            'entity_type' => 'BusinessApplication',
            'entity_id' => $application->id,
            'details' => "Application submitted for business '{$application->business_name}' by {$application->email}",
            'ip_address' => $request->ip(),
        ]);

        return response()->json([
            'message' => 'Your business application has been submitted and is pending review.',
            'application_id' => $application->id,
            'status' => 'PENDING',
            'created_at' => $application->created_at,
        ], 201);
    }

    public function showStatus(string $id)
    {
        $application = BusinessApplication::findOrFail($id);

        return response()->json([
            'id' => $application->id,
            'business_name' => $application->business_name,
            'owner_name' => $application->owner_name,
            'status' => $application->status,
            'rejection_reason' => $application->rejection_reason,
            'created_at' => $application->created_at,
        ]);
    }
}
