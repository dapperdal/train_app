# Train App - Bug Fixes Summary

## Issues Identified

### 1. **No Journey State Persistence** ❌ FIXED
**Problem**: When user clicks "I'm Onboard" and starts a journey, the journey state was only stored in memory (RAM).
- If the app was closed or recreated, all journey data was lost
- Progress tracking couldn't resume
- No way to recover an active journey if the app crashed

**Solution**: 
- Created `JourneyPersistenceManager` to save/restore journeys using DataStore
- Journey is now saved to disk when started
- Journey is automatically restored when app restarts
- Journey is cleared when completed or ended

### 2. **No State Restoration on App Restart** ❌ FIXED
**Problem**: The ViewModel had no code to check for previously saved journeys.
- ViewModel initialized fresh every time
- Active journeys couldn't be restored

**Solution**:
- Added `restoreActiveJourney()` method called in ViewModel init
- Automatically restores and resumes journey tracking on app restart
- Resumes live updates and weather fetching

### 3. **Updates Not Being Persisted** ❌ FIXED
**Problem**: Even while a journey was active, updates weren't being saved.
- Progress calculations happened but weren't persisted
- If app crashed mid-journey, all progress was lost

**Solution**:
- Journey state is now saved every 30 seconds (during live updates)
- Saved whenever progress is recalculated
- Cleared only when journey completes or user ends it

## Files Modified

### New File
- `app/src/main/java/com/darren/trains/service/JourneyPersistenceManager.kt`
  - Handles all save/load operations using DataStore
  - Uses Gson for JSON serialization

### Updated Files
- `app/src/main/java/com/darren/trains/viewmodel/JourneyViewModel.kt`
  - Added dependency injection for JourneyPersistenceManager
  - Added init block to restore journeys on startup
  - Updated startJourney() to persist journey
  - Updated endJourney() to clear saved journey
  - Updated startLiveUpdates() to save on every update

- `app/src/main/java/com/darren/trains/di/AppModule.kt`
  - Added Gson provider for JSON serialization

## How It Works Now

### Journey Lifecycle with Persistence

1. **User Clicks "I'm Onboard"**
   - Journey is created in memory
   - **Immediately saved to disk**
   - Live updates start (every 30 seconds)

2. **Every 30 Seconds**
   - Progress is recalculated
   - **Journey is saved to disk with latest state**
   - Weather updated if needed

3. **App is Closed/Crashes**
   - User can close app without losing progress
   - Next time app starts, journey is restored
   - Progress tracking resumes seamlessly

4. **Journey Completes (Arrives at Destination)**
   - Journey moves to "Completed" state
   - **Saved journey is cleared from disk**
   - User sees completion screen

5. **User Ends Journey Early**
   - **Saved journey is cleared from disk**
   - Back to home screen

## Testing Recommendations

### Test Case 1: Basic Persistence
1. Start a journey
2. Close the app completely
3. Reopen the app
4. **Expected**: Journey should resume with same state

### Test Case 2: Progress Updates
1. Start a journey
2. Wait 30 seconds
3. Check that "stops remaining" and "arrival time" are updating
4. Close app
5. Reopen app
6. **Expected**: Progress should have advanced, and be restored correctly

### Test Case 3: Journey Completion
1. Start a journey on a train that arrives soon
2. Close app
3. Reopen when train would have arrived
4. **Expected**: Should show completion screen, journey data cleared

### Test Case 4: Crash Recovery
1. Start a journey
2. Force close app (through device settings)
3. Reopen app
4. **Expected**: Journey should be restored

## Notes

- Journeys are stored in encrypted SharedPreferences via DataStore
- Only one active journey can be stored at a time
- Journey data includes all calling points for accurate progress calculation
- Gson is used for serialization (already in build.gradle dependencies)
