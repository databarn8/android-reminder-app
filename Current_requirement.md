# Current Requirements

## Active Development Tasks
- Fix datetime save issue - update whenDay/whenTime from selectedDate/selectedTime ✅ COMPLETED
- Ensure proper race condition handling in database operations ✅ COMPLETED
- Implement enhanced logging for debugging datetime issues ✅ COMPLETED

## Build System Requirements
**IMPORTANT**: Always read `fix-process.md` file first to gain knowledge of specific build system fixes before making any changes.

## Documentation Requirements
**IMPORTANT**: Always add fix details to `fix-process.md` with timestamp for every build-related change made.

## GitHub Branch Management Requirements
**IMPORTANT**: Always follow proper timestamped branch workflow for GitHub pushes.

### Branch Creation Process:

#### Step 1: Get Current Time
```bash
date
# Example output: Sat Nov 15 11:00:50 AM EST 2025
```

#### Step 2: Create Timestamped Branch
```bash
git checkout -b [type]-[description]-YYYY-MM-DD-HHMM
```

#### Step 3: Push to GitHub
```bash
git push origin [type]-[description]-YYYY-MM-DD-HHMM
```

### Branch Naming Convention:
- **Work Branches**: `work-[feature]-YYYY-MM-DD-HHMM`
- **Fix Branches**: `fix-[issue]-YYYY-MM-DD-HHMM`  
- **Feature Branches**: `feature-[name]-YYYY-MM-DD-HHMM`
- **Hotfix Branches**: `hotfix-[urgent]-YYYY-MM-DD-HHMM`

### Examples:
```bash
# Work branch (current time 11:00)
git checkout -b work-datetime-display-fix-2025-11-15-1100

# Fix branch (current time 14:30)
git checkout -b fix-login-crash-2025-11-15-1430

# Feature branch (current time 09:15)
git checkout -b feature-voice-commands-2025-11-15-0915

# Hotfix branch (current time 16:45)
git checkout -b hotfix-server-down-2025-11-15-1645
```

### Critical Rules:
1. **NEVER use fixed times** like 1500 - always use ACTUAL current time
2. **ALWAYS use 24-hour format** (HHMM from `date` command)
3. **MUST include date** (YYYY-MM-DD) for chronological sorting
4. **PUSH IMMEDIATELY** after creating branch to establish timestamp
5. **DOCUMENT in fix-process.md** with same timestamp
6. **ROLLBACK when needed** - don't keep fixing broken implementations

### Benefits:
- **Easy Rollback**: `git checkout work-datetime-display-fix-2025-11-15-1100`
- **Clear Timeline**: Branches sorted chronologically by name
- **Precise Tracking**: Know exactly when work was done
- **Team Coordination**: Multiple developers can work in parallel
- **Release Management**: Easy to track which fixes went into which releases

### Workflow Summary:
1. **Check time**: `date` → get current HHMM
2. **Create branch**: `git checkout -b work-task-2025-11-15-1100`
3. **Do work**: Make changes, test, commit
4. **Push branch**: `git push origin work-task-2025-11-15-1100`
5. **Document**: Add to fix-process.md with timestamp
6. **Repeat**: Create new branch for next task

## Current Issues

### 1. Datetime Save Fix Status: PARTIALLY WORKING ✅❌
✅ **Working**: 
- Saves new datetime values correctly
- Preserves existing data (doesn't lose it)
- No race conditions during save operations

❌ **Not Working**:
- Edit screen doesn't display existing datetime values from database
- UI shows blank datetime fields even when data exists
- User can't see what datetime is currently set

**Root Cause**: Display/loading issue in InputScreen.kt - data loads but doesn't populate UI fields properly

### 2. Fresh Button Feature: FAILED - ROLLED BACK ❌
❌ **Failed**: 
- Fresh button caused system crashes and app instability
- ScreenFlashOverlay integration created compilation errors
- App became unstable after implementation

**Action Taken**: 
- Rolled back to commit `77bbf27` (working datetime save state)
- Created stable branch `work-stable-rollback-2025-11-15-1110`
- Stashed broken Fresh button implementation for future analysis

**Root Cause**: 
- ScreenFlashOverlay may have system overlay permission issues
- Possible conflicts with existing Compose UI structure
- Need alternative approach for screen flash functionality

**Current Status**: Back to stable working state
**Next Step**: Research alternative screen flash methods and reimplement Fresh button safely

## Next Steps
- Research alternative screen flash techniques using Tavily
- Implement Fresh button with simpler approach (no overlays)
- Fix remaining compilation errors in other files
- Test complete datetime functionality on device
- Implement any additional datetime-related features if needed