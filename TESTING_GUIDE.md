# Darren's Trains - Comprehensive Testing Guide

## Overview
This guide covers testing the C2C Network implementation with set-and-forget setup, location-based smart routing, and advanced features.

---

## Part 1: Setup & First Launch

### Test 1.1: First-Time App Launch (No Setup)
**Steps:**
1. Uninstall app (or clear app data)
2. Launch app
3. Verify SetupScreen appears with:
   - Step 1: "Select Home Station"
   - Step 2: "Select Work Station" 
   - Step 3: "Preferences"
   - Progress indicator showing completion

**Expected Result:** SetupScreen displays all 23 C2C stations in list, progress indicator shows initial step

### Test 1.2: Home Station Selection
**Steps:**
1. From SetupScreen home station view
2. Scroll and select "Leigh-on-Sea (LES)"
3. Verify checkmark appears next to selection
4. Click "Next" or progress to work station

**Expected Result:** Selection saves to temp state, UI updates with checkmark, navigation to next step works

### Test 1.3: Work Station Selection
**Steps:**
1. Select "Fenchurch Street (FST)" as work station
2. Verify different from home station (validation works)
3. Try selecting home station again (FST) - should still be disabled/invalid
4. Proceed to next step

**Expected Result:** Cannot select same station twice, progress indicator advances, next step loads

### Test 1.4: Save Setup Preferences
**Steps:**
1. Complete both home and work station selection
2. Toggle "Filter Lakeside Loop" on/off
3. Click "Save" or "Confirm"
4. Verify preferences saved to DataStore
5. Relaunch app - SetupScreen should NOT appear

**Expected Result:** HomeScreen loads directly (setup skipped on subsequent launches), preferences persisted

### Test 1.5: Setup Reset
**Steps:**
1. After completing setup, navigate to Settings
2. Look for "Reset Setup" or similar option
3. Clear preferences and relaunch app

**Expected Result:** SetupScreen appears again, DataStore cleared

---

## Part 2: Location Detection

### Test 2.1: Location Permission Request
**Steps:**
1. Launch app after setup
2. Check system notifications for location permission request
3. Grant permission to app
4. Verify LocationHelper begins detecting location

**Expected Result:** Permission dialog appears, location detection activates after grant

### Test 2.2: Current Station Detection (At Leigh-on-Sea)
**Prerequisite:** You must be at or near Leigh-on-Sea station
**Steps:**
1. Launch app (with location permission)
2. Wait 5-10 seconds for location detection
3. Verify "Currently at: Leigh-on-Sea (LES)" appears in header
4. Verify "Showing trains to: Fenchurch Street (FST)" below

**Expected Result:** App correctly identifies Leigh-on-Sea as nearest station (distance < 1 mile), displays smart routing

**Debug if not working:**
- Check logcat: `LocationHelper` debug logs showing detected station
- Verify device has valid GPS fix (try opening Maps app first)
- Check DataStore has correct home/work stations saved
- Review `calculateDistance()` in LocationHelper for Haversine calculation

### Test 2.3: Current Station Detection (At Fenchurch Street)
**Prerequisite:** You must be at or near Fenchurch Street station
**Steps:**
1. Launch app (with location permission)
2. Wait for location detection
3. Verify header shows "Currently at: Fenchurch Street (FST)"
4. Verify destination shows "Showing trains to: Leigh-on-Sea (LES)"

**Expected Result:** Smart routing inverts when at work station

**Debug if direction inverted:**
- Check MainViewModel logic in `fetchTrains()`:
  ```kotlin
  when (currentStationCrs) {
      primaryRoute.homeStationCrs -> primaryRoute.workStationCrs
      primaryRoute.workStationCrs -> primaryRoute.homeStationCrs
      else -> primaryRoute.workStationCrs
  }
  ```

### Test 2.4: Current Station Detection (At Intermediate Station)
**Prerequisite:** You must be at an intermediate station (e.g., Grays, Rochford, Southend)
**Steps:**
1. Launch app from intermediate station
2. Verify "Currently at: [Station Name]" shows correct station
3. Verify destination defaults to work station
4. Pull to refresh - trains should fetch FROM current location TO work station

**Expected Result:** App correctly identifies intermediate station, defaults to showing trains to saved work station

### Test 2.5: Location Permission Denied
**Steps:**
1. Clear app data
2. Launch app and deny location permission
3. Verify app still works but shows fallback UI
4. Check HomeScreen shows general info instead of current station

**Expected Result:** App gracefully handles no-permission scenario, shows legacy location status UI

---

## Part 3: Smart Routing

### Test 3.1: Bidirectional Routing (LES → FST)
**Steps:**
1. At Leigh-on-Sea, verify trains show "to: Fenchurch Street"
2. Fetch departures
3. Verify first few trains have FST-bound schedules

**Expected Result:** API call uses `fromCrs=LES&toCrs=FST`, returns correct departures

### Test 3.2: Bidirectional Routing (FST → LES)
**Steps:**
1. At Fenchurch Street, verify trains show "to: Leigh-on-Sea"
2. Fetch departures
3. Verify first few trains have LES-bound schedules

**Expected Result:** API call uses `fromCrs=FST&toCrs=LES`, returns correct departures

### Test 3.3: Change Destination in Settings
**Steps:**
1. Open Settings screen (gear icon)
2. Change work station from FST to "Southend Victoria (SOV)"
3. Save changes
4. Return to HomeScreen
5. Verify "Showing trains to: Southend Victoria" in header
6. Refresh to confirm trains fetch to new destination

**Expected Result:** Destination updates immediately, API calls use new destination CRS

### Test 3.4: Verify Correct Trains Returned
**Steps:**
1. Note current location and configured destination
2. Pull to refresh
3. Check train list - all should be bound for configured destination
4. Open one train detail and verify calling points include destination station

**Expected Result:** No trains shown bound for wrong direction

---

## Part 4: Train Filtering

### Test 4.1: Lakeside Loop Filter (Enabled)
**Steps:**
1. Go to Settings
2. Enable "Filter Lakeside Loop"
3. Return to HomeScreen and refresh
4. Count visible trains
5. Note any trains that pass through Grays (GRY) or Tilbury (TIL)

**Expected Result:** No trains with GRY/TIL in calling points visible

**Debug if Lakeside trains still appear:**
- Check TrainRepository `hasIntermediateStation()` method
- Verify filter logic in `getDepartures()`:
  ```kotlin
  ?.filter { service ->
      if (filterLakesideLoop) !hasIntermediateStation(service, "GRY", "TIL")
      else true
  }
  ```
- Check Huxley API response - confirm calling points include CRS codes

### Test 4.2: Lakeside Loop Filter (Disabled)
**Steps:**
1. Go to Settings
2. Disable "Filter Lakeside Loop"
3. Return to HomeScreen and refresh
4. Count visible trains

**Expected Result:** More trains visible (Lakeside services included)

### Test 4.3: Grays Service Filter (Always)
**Steps:**
1. At any time, check if trains stopping ONLY at Grays visible
2. Pure Grays services should never appear (always filtered)

**Expected Result:** No trains with only Grays stop

### Test 4.4: Cancelled Trains Filter
**Steps:**
1. During disruptions (peak hours), observe train list
2. Check status column - all visible trains should have non-cancelled status
3. Pull to refresh during a service disruption

**Expected Result:** No trains marked as "CANCELLED" shown

---

## Part 5: Advanced Features

### Test 5.1: Prefer Express Trains
**Steps:**
1. Go to Settings
2. Enable "Prefer Express Trains"
3. Save and return to HomeScreen
4. Note train order - should be sorted by calling points (fewest stops first)
5. Observe first train has fewer calling points than before

**Expected Result:** Trains sorted by `callingPoints.size`, fewer stops first

**Verify:**
- Check TrainRepository sorting logic:
  ```kotlin
  if (preferExpressTrains) {
      departures = departures.sortedBy { it.callingPoints.size }
  }
  ```

### Test 5.2: Notification on Delay
**Steps:**
1. Go to Settings
2. Enable "Notify on Delay"
3. Board a train
4. If train gets delayed, verify notification appears (may require journey tracking)

**Expected Result:** Notifications only appear when enabled AND delay > 5 minutes

### Test 5.3: Max Connection Time
**Steps:**
1. Go to Settings  
2. Note "Max Connection Time" setting (future UI addition)
3. Trains exceeding this time filtered out (currently 15 mins default)

**Expected Result:** Trains with `journeyTimeMinutes > maxChangeTime * 6` filtered

---

## Part 6: Favorite Routes (Future)

### Test 6.1: Add Favorite Route
**Steps:**
1. From HomeScreen, look for "Save as Favorite" or add button
2. Save current route with custom name
3. Verify it appears in favorites list

**Expected Result:** Route saved to DataStore, appears in quick access list

### Test 6.2: Switch Between Routes
**Steps:**
1. Tap a saved favorite route
2. Verify from/to stations update
3. Verify trains fetch for new route
4. Confirm UI shows new stations in header

**Expected Result:** Quick switching works without going to Settings

---

## Part 7: Pull-to-Refresh

### Test 7.1: Manual Refresh
**Steps:**
1. From HomeScreen, pull down from top
2. Observe refresh indicator spins
3. Wait for data to reload
4. Check "Last updated" timestamp changed

**Expected Result:** Pull-refresh triggers new API call, timestamp updates

### Test 7.2: Auto-Refresh
**Steps:**
1. Leave HomeScreen open for 2+ minutes
2. Observe trains updating automatically every 60 seconds
3. Check "Last updated" time advances

**Expected Result:** Data refreshes every 60 seconds without user action

---

## Part 8: Error Handling

### Test 8.1: Network Error
**Steps:**
1. Disable network (airplane mode or WiFi off)
2. Pull to refresh
3. Verify error message appears
4. Re-enable network and try again

**Expected Result:** Error card displayed, user can retry, recovers on reconnect

### Test 8.2: API Error (500)
**Steps:**
1. Simulate network error (impossible to do without mock server)
2. Observe how app handles failed API call

**Expected Result:** Error message shown, doesn't crash, user can retry

### Test 8.3: Location Error
**Steps:**
1. Deny location permission
2. Verify app works with fallback UI
3. Add location permission later via settings

**Expected Result:** Graceful degradation without location

---

## Part 9: Edge Cases

### Test 9.1: Same Home and Work Station
**Steps:**
1. Go to Settings
2. Try to set same station for home and work
3. Verify "Next" button disabled or shows validation error

**Expected Result:** Cannot save invalid configuration

### Test 9.2: Empty Train List
**Steps:**
1. Query at off-peak hours where no trains available
2. Verify "No trains available" message shows
3. Try refreshing periodically

**Expected Result:** Graceful message, not blank screen or crash

### Test 9.3: Journey Tracking
**Steps:**
1. From HomeScreen, tap "Board" on a train
2. Verify JourneyTrackingScreen appears
3. Monitor progress as journey updates
4. Complete journey

**Expected Result:** Journey tracking works with smart routing trains

### Test 9.4: Settings Screen Navigation
**Steps:**
1. From HomeScreen, tap settings gear icon
2. Verify SettingsScreen loads
3. Make changes
4. Tap back/cancel - verify changes not saved or saved per button
5. Tap save - verify HomeScreen loads with new settings

**Expected Result:** Navigation and preference persistence works

---

## Part 10: Performance Testing

### Test 10.1: App Launch Time
**Steps:**
1. Force stop app
2. Measure time from launch to HomeScreen appears
3. Should be < 3 seconds with setup cached

**Expected Result:** Fast startup with DataStore-cached preferences

### Test 10.2: Scroll Performance
**Steps:**
1. HomeScreen with 15+ trains loaded
2. Scroll smoothly through list
3. No jank or dropped frames

**Expected Result:** Smooth 60fps scrolling (check with developer tools)

### Test 10.3: Location Detection Frequency
**Steps:**
1. Watch logcat for LocationHelper logs
2. Verify location queried on app launch
3. Verify not queried excessively (drain battery)

**Expected Result:** Location checked on startup, then only when user refreshes

---

## Debugging Guide

### Enable Verbose Logging

Add debug logs to key components:

**LocationHelper.kt:**
```kotlin
Log.d("LocationHelper", "Detected station: ${nearestStation?.name} at distance $distance miles")
```

**MainViewModel.kt:**
```kotlin
Log.d("MainViewModel", "Smart routing: from=$currentStationCrs to=$destinationCrs")
```

**TrainRepository.kt:**
```kotlin
Log.d("TrainRepository", "Fetching trains: fromCrs=$fromCrs, toCrs=$toCrs, filterLakeside=$filterLakesideLoop")
```

### Inspect DataStore
Use Android Studio Logcat:
```
adb shell cat /data/data/com.darren.trains/files/datastore/station_preferences.preferences_pb
```

Or use DataStore Inspector in Android Studio Device Explorer.

### Test Specific Stations
Create test locations at different C2C stations:
- **Leigh-on-Sea (LES):** 51.5402, 0.6326
- **Southend Victoria (SOV):** 51.6150, 0.7130
- **Grays (GRY):** 51.4567, 0.3654
- **Fenchurch Street (FST):** 51.5143, -0.0754

Use Android Emulator "Extended Controls" → "Location" to simulate GPS.

### Common Issues

| Issue | Diagnosis | Fix |
|-------|-----------|-----|
| App crashes on launch | Check Hilt dependency injection | Verify `@AndroidEntryPoint` and `@Inject` annotations |
| Trains don't update | Check network/API | Run `adb logcat` and look for API errors |
| Wrong station detected | GPS accuracy low | Ensure strong GPS signal, try another location |
| Settings not persisting | DataStore not saving | Check exception handling in `updateAdvancedSettings()` |
| Favorite routes missing | Gson serialization error | Verify `gson?.toJson()` doesn't fail silently |

---

## Regression Testing Checklist

After each code change, verify:

- [ ] App launches without crashing
- [ ] Setup screen appears on first launch only
- [ ] Location detection works (at least one test location)
- [ ] Smart routing shows correct destination
- [ ] Lakeside filter toggles work
- [ ] Pull-to-refresh triggers new data
- [ ] Settings screen updates persist
- [ ] No trains marked as cancelled
- [ ] Journey tracking still works
- [ ] No unexpected crashes in logcat

---

## User Acceptance Testing

Have real users test:

1. **First impressions:** Is setup intuitive?
2. **Location accuracy:** Does detected station match their actual location?
3. **Train accuracy:** Do shown trains match real schedule?
4. **Preferred filtering:** Are their filtered preferences working?
5. **Performance:** Is app snappy?
6. **Reliability:** Does it crash? Does location detection fail?

Gather feedback on:
- What features they use most
- What filtering options matter
- Whether "set and forget" achieves the goal
- Any confusing UI elements

---

## Future Enhancements to Test

- [ ] Operator filtering UI (show only C2C trains)
- [ ] Connection time validation for multi-leg journeys
- [ ] Real-time disruption alerts
- [ ] Integration with journey tracking for crowding data
- [ ] Predictive "you'll be late" notifications
