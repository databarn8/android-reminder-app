# Email Client Preference Feature Requirements

## Overview
Enhance the email sending functionality to remember and use the last selected email client/account by default, while still allowing users to change their selection when needed.

## Current Behavior
- App shows a chooser dialog with all available email clients
- User must select an email client every time they send an email
- No memory of previous selections

## Desired Behavior
1. Remember the last used email client/account
2. Use the remembered client as the default for future emails
3. Provide an easy way to switch to a different client/account
4. Store this preference persistently

## Implementation Details

### 1. Data Storage
- Add email client preference to SharedPreferences or database
- Store package name of the selected email client
- Store email account if available (for clients that support multiple accounts)

### 2. Email Service Enhancement
- Modify existing email service classes to check for saved preference
- Fall back to chooser dialog if no preference exists
- Update preference after successful email sending

### 3. UI Components
- Add "Change Email Client" option in email-related screens
- Display current email client/account being used
- Provide clear indication when default client is being used

### 4. Files to Modify
1. `EmailService.kt` (or similar email service files)
   - Add preference checking logic
   - Implement default client selection
   - Update preference after sending

2. `EmailPreferences.kt` (new file)
   - Manage email client preferences
   - Handle saving/loading preferences
   - Provide methods to update/clear preferences

3. UI screens that trigger email sending
   - Add option to change email client
   - Display current email client preference

### 5. Implementation Steps
1. Create EmailPreferences data class and manager
2. Modify email service to use preferences
3. Update UI to show current email client
4. Add option to change email client
5. Test with various email clients (Gmail, Outlook, etc.)

### 6. Edge Cases to Handle
- Email client uninstalled from device
- Multiple accounts in same email client
- First-time usage (no preference saved)
- User cancels email sending

### 7. Testing Requirements
- Test with different email clients installed
- Test preference persistence across app restarts
- Test changing email client preference
- Test behavior when preferred client is uninstalled

## Technical Considerations
- Use Android's Email Intent for compatibility
- Handle package name validation
- Ensure preference storage is secure and reliable
- Consider backup/restore of preferences