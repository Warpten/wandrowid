package org.warpten.wandrowid;

/**
 * An interface that serves as a way for fragments to inform activities.
 * Example:
 *   private OnAuthFragmentListener mListener;
 *   public void onAttach(Activity activity) { mListener = activity; }
 *   // onAttach is called when the fragment is inflated by the activity.
 */
public interface OnAuthFragmentListener {
    public void OnRealmSelected(String realmName, String address, int port);
}