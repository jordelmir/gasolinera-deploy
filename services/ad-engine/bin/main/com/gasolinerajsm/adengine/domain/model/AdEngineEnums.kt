package com.gasolinerajsm.adengine.domain.model

/**
 * Advertisement type enumeration
 */
enum class AdType(val displayName: String, val description: String) {
    BANNER("Banner Ad", "Static or animated banner advertisement"),
    VIDEO("Video Ad", "Video advertisement with play controls"),
    INTERSTITIAL("Interstitial Ad", "Full-screen advertisement"),
    NATIVE("Native Ad", "Advertisement that matches app content style"),
    REWARDED_VIDEO("Rewarded Video", "Video ad that provides rewards upon completion"),
    PLAYABLE("Playable Ad", "Interactive advertisement with mini-game"),
    AUDIO("Audio Ad", "Audio-only advertisement"),
    RICH_MEDIA("Rich Media", "Interactive multimedia advertisement");

    /**
     * Check if ad type supports video content
     */
    fun supportsVideo(): Boolean {
        return this == VIDEO || this == INTERSTITIAL || this == REWARDED_VIDEO || this == PLAYABLE
    }

    /**
     * Check if ad type supports interaction
     */
    fun supportsInteraction(): Boolean {
        return this == INTERSTITIAL || this == NATIVE || this == PLAYABLE || this == RICH_MEDIA
    }

    /**
     * Check if ad type typically provides rewards
     */
    fun typicallyProvidesRewards(): Boolean {
        return this == REWARDED_VIDEO || this == PLAYABLE
    }

    /**
     * Get minimum duration for this ad type
     */
    fun getMinimumDurationSeconds(): Int {
        return when (this) {
            VIDEO, REWARDED_VIDEO -> 15
            INTERSTITIAL -> 5
            PLAYABLE -> 30
            AUDIO -> 10
            else -> 3
        }
    }

    /**
     * Get maximum duration for this ad type
     */
    fun getMaximumDurationSeconds(): Int {
        return when (this) {
            VIDEO, REWARDED_VIDEO -> 60
            INTERSTITIAL -> 15
            PLAYABLE -> 120
            AUDIO -> 30
            else -> 10
        }
    }
}

/**
 * Advertisement status enumeration
 */
enum class AdStatus(val displayName: String, val description: String) {
    DRAFT("Draft", "Advertisement is being prepared"),
    PENDING_APPROVAL("Pending Approval", "Advertisement is awaiting approval"),
    ACTIVE("Active", "Advertisement is running"),
    PAUSED("Paused", "Advertisement is temporarily paused"),
    COMPLETED("Completed", "Advertisement campaign has ended"),
    CANCELLED("Cancelled", "Advertisement has been cancelled"),
    REJECTED("Rejected", "Advertisement was rejected during approval"),
    EXPIRED("Expired", "Advertisement has expired");

    /**
     * Check if status allows serving ads
     */
    fun allowsServing(): Boolean {
        return this == ACTIVE
    }

    /**
     * Check if status allows modifications
     */
    fun allowsModifications(): Boolean {
        return this == DRAFT || this == PAUSED || this == REJECTED
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == CANCELLED || this == REJECTED || this == EXPIRED
    }

    /**
     * Check if status can transition to another status
     */
    fun canTransitionTo(newStatus: AdStatus): Boolean {
        return when (this) {
            DRAFT -> newStatus in listOf(PENDING_APPROVAL, ACTIVE, CANCELLED)
            PENDING_APPROVAL -> newStatus in listOf(ACTIVE, REJECTED, CANCELLED)
            ACTIVE -> newStatus in listOf(PAUSED, COMPLETED, CANCELLED, EXPIRED)
            PAUSED -> newStatus in listOf(ACTIVE, COMPLETED, CANCELLED, EXPIRED)
            COMPLETED, CANCELLED, REJECTED, EXPIRED -> false // Final states
        }
    }
}

/**
 * Engagement type enumeration
 */
enum class EngagementType(val displayName: String, val description: String) {
    IMPRESSION("Impression", "Advertisement was displayed to user"),
    VIEW("View", "User actively viewed the advertisement"),
    CLICK("Click", "User clicked on the advertisement"),
    INTERACTION("Interaction", "User interacted with the advertisement"),
    COMPLETION("Completion", "User completed viewing the advertisement");

    /**
     * Check if engagement type is billable
     */
    fun isBillable(): Boolean {
        return this == IMPRESSION || this == CLICK || this == COMPLETION
    }

    /**
     * Check if engagement type indicates user interest
     */
    fun indicatesInterest(): Boolean {
        return this == CLICK || this == INTERACTION || this == COMPLETION
    }

    /**
     * Get typical reward multiplier for this engagement type
     */
    fun getRewardMultiplier(): Double {
        return when (this) {
            IMPRESSION -> 1.0
            VIEW -> 1.2
            CLICK -> 1.5
            INTERACTION -> 1.8
            COMPLETION -> 2.0
        }
    }
}

/**
 * Engagement status enumeration
 */
enum class EngagementStatus(val displayName: String, val description: String) {
    STARTED("Started", "Engagement has started"),
    VIEWED("Viewed", "Advertisement was viewed"),
    INTERACTED("Interacted", "User interacted with advertisement"),
    COMPLETED("Completed", "Engagement was completed successfully"),
    SKIPPED("Skipped", "User skipped the advertisement"),
    ABANDONED("Abandoned", "User abandoned the engagement"),
    ERROR("Error", "An error occurred during engagement"),
    TIMEOUT("Timeout", "Engagement timed out");

    /**
     * Check if status indicates successful engagement
     */
    fun isSuccessful(): Boolean {
        return this == COMPLETED || this == VIEWED || this == INTERACTED
    }

    /**
     * Check if status is a final state
     */
    fun isFinalState(): Boolean {
        return this == COMPLETED || this == SKIPPED || this == ABANDONED ||
               this == ERROR || this == TIMEOUT
    }

    /**
     * Check if status allows further interaction
     */
    fun allowsInteraction(): Boolean {
        return this == STARTED || this == VIEWED || this == INTERACTED
    }
}

/**
 * Billing event enumeration
 */
enum class BillingEvent(val displayName: String, val description: String) {
    IMPRESSION("Impression", "Charged per impression"),
    CLICK("Click", "Charged per click"),
    COMPLETION("Completion", "Charged per completion"),
    INTERACTION("Interaction", "Charged per interaction"),
    TIME_BASED("Time Based", "Charged based on view time");

    /**
     * Check if billing event is performance-based
     */
    fun isPerformanceBased(): Boolean {
        return this == CLICK || this == COMPLETION || this == INTERACTION
    }

    /**
     * Get typical cost multiplier for this billing event
     */
    fun getCostMultiplier(): Double {
        return when (this) {
            IMPRESSION -> 1.0
            CLICK -> 5.0
            COMPLETION -> 10.0
            INTERACTION -> 7.5
            TIME_BASED -> 2.0
        }
    }
}

/**
 * User segment enumeration for targeting
 */
enum class UserSegment(val displayName: String, val description: String) {
    NEW_USER("New User", "Recently registered users"),
    ACTIVE_USER("Active User", "Regularly active users"),
    PREMIUM_USER("Premium User", "Users with premium subscriptions"),
    FREQUENT_BUYER("Frequent Buyer", "Users who make frequent purchases"),
    HIGH_SPENDER("High Spender", "Users with high spending patterns"),
    LOYALTY_MEMBER("Loyalty Member", "Members of loyalty programs"),
    MOBILE_USER("Mobile User", "Primarily mobile app users"),
    DESKTOP_USER("Desktop User", "Primarily desktop users"),
    COMMUTER("Commuter", "Users who commute regularly"),
    WEEKEND_USER("Weekend User", "Users active mainly on weekends");

    /**
     * Check if segment indicates high value user
     */
    fun isHighValue(): Boolean {
        return this in listOf(PREMIUM_USER, HIGH_SPENDER, FREQUENT_BUYER, LOYALTY_MEMBER)
    }

    /**
     * Check if segment indicates engagement pattern
     */
    fun indicatesEngagement(): Boolean {
        return this in listOf(ACTIVE_USER, FREQUENT_BUYER, LOYALTY_MEMBER)
    }
}

/**
 * Device type enumeration
 */
enum class DeviceType(val displayName: String) {
    MOBILE("Mobile"),
    TABLET("Tablet"),
    DESKTOP("Desktop"),
    TV("Smart TV"),
    KIOSK("Kiosk"),
    UNKNOWN("Unknown");

    /**
     * Check if device type is mobile
     */
    fun isMobile(): Boolean {
        return this == MOBILE || this == TABLET
    }

    /**
     * Check if device type supports touch interaction
     */
    fun supportsTouchInteraction(): Boolean {
        return this == MOBILE || this == TABLET || this == KIOSK
    }
}

/**
 * Gender enumeration for targeting
 */
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromString(value: String): Gender? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * Day of week enumeration for scheduling
 */
enum class DayOfWeek(val displayName: String, val shortName: String) {
    MONDAY("Monday", "MON"),
    TUESDAY("Tuesday", "TUE"),
    WEDNESDAY("Wednesday", "WED"),
    THURSDAY("Thursday", "THU"),
    FRIDAY("Friday", "FRI"),
    SATURDAY("Saturday", "SAT"),
    SUNDAY("Sunday", "SUN");

    /**
     * Check if day is weekend
     */
    fun isWeekend(): Boolean {
        return this == SATURDAY || this == SUNDAY
    }

    /**
     * Check if day is weekday
     */
    fun isWeekday(): Boolean {
        return !isWeekend()
    }

    companion object {
        fun fromShortName(shortName: String): DayOfWeek? {
            return values().find { it.shortName.equals(shortName, ignoreCase = true) }
        }
    }
}