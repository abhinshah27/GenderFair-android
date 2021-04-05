/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package com.groops.fairsquare.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.groops.fairsquare.R;
import com.groops.fairsquare.models.HLUserGeneric;
import com.groops.fairsquare.models.Interest;
import com.groops.fairsquare.models.InterestCategory;
import com.groops.fairsquare.models.Post;
import com.groops.fairsquare.models.PostList;
import com.groops.fairsquare.models.UsersBundle;
import com.groops.fairsquare.models.enums.GlobalSearchTypeEnum;
import com.groops.fairsquare.models.enums.MemoryColorEnum;
import com.groops.fairsquare.utility.Utils;
import com.groops.fairsquare.utility.helpers.MediaHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public class GlobalSearchListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final int TYPE_CARDS = 0;
	private final int TYPE_SQUARED = 1;
	private final int TYPE_CIRCLES = 2;

	/**
	 * @since 4/16/2019: needed to tell the {@link GlobalSearchListsAdapter} to show a {@link android.widget.Button}.
	 */
	private final int TYPE_BUTTON = 3;

	private List<GlobalSearchObject> items;

	private OnGlobalSearchActionListener mSearchListener;

	public GlobalSearchListsAdapter(List<GlobalSearchObject> items, OnGlobalSearchActionListener listener) {
		this.items = items;
		this.mSearchListener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		RecyclerView.ViewHolder vh;
		switch (viewType) {
			case TYPE_CARDS:
				vh = new PostListVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_my_diary_main, parent, false), mSearchListener);
				break;

			case TYPE_SQUARED:
				vh = new InterestPeopleContainerVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_global_search_squared, parent, false),
						mSearchListener);
				break;

			case TYPE_CIRCLES:
                vh = new InterestPeopleContainerScrollViewVH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_global_search_circles, parent, false),
                        mSearchListener);
				break;

			case TYPE_BUTTON:
				vh = new ShopButtonVH(new TextView(parent.getContext()), mSearchListener);
				break;

			default:
				vh = new PostListVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_my_diary_main, parent, false), mSearchListener);
		}

		return vh;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		GlobalSearchObject bundle = items.get(position);
		if (bundle != null) {
			switch (getItemViewType(position)) {
				case TYPE_SQUARED:
                case TYPE_CIRCLES:
					if (bundle.isUsers() || bundle.isInterests()) {

					    if (getItemViewType(position) == TYPE_SQUARED) {
                            InterestPeopleContainerVH vHolder = ((InterestPeopleContainerVH) holder);
                            vHolder.setItem(bundle.getMainObject());
                            vHolder.setObjectType(bundle.getObjectType());
                        }
                        else {
                            InterestPeopleContainerScrollViewVH vHolder1 = ((InterestPeopleContainerScrollViewVH) holder);
                            vHolder1.setCirclesList(bundle.getMainObject(), position);
                            vHolder1.setObjectType(bundle.getObjectType());
                        }

//						if (position > 0) {
//							int paddingLarge = holder.itemView.getResources().getDimensionPixelSize(R.dimen.activity_margin_lg);
//
//							holder.itemView.setPadding(
//									paddingLarge,
//									Utils.dpToPx(10f, holder.itemView.getResources()),
//									paddingLarge,
//									paddingLarge
//							);
//						}
					}
					break;

				case TYPE_CARDS:
					if (bundle.isPosts()) {
						PostListVH vHolder1 = ((PostListVH) holder);
						vHolder1.setPostList(((PostList) bundle.getMainObject()), bundle.getReturnType(), position);
					}
					break;

				case TYPE_BUTTON:
					// no ops
					break;
			}
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}

	@Override
	public int getItemViewType(int position) {
		GlobalSearchObject obj = items.get(position);
		if (obj != null) {

			if (position == getItemCount() - 1 && obj.isButton())
				return TYPE_BUTTON;

			switch(obj.getUIType()) {
				case SQUARED:
					return TYPE_SQUARED;
				case CARDS:
					return TYPE_CARDS;
				case CIRCLES:
					return TYPE_CIRCLES;
			}
		}
		return super.getItemViewType(position);
	}

	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link PostList}.
	 */
	static class PostListVH extends RecyclerView.ViewHolder {

		private final TextView listName;
		private final HorizontalScrollView postsScrollView;
		private final ViewGroup postsContainer;
		private final TextView noResult;

		private String returnTypeString;

		private final OnGlobalSearchActionListener mListener;

		PostListVH(View itemView, OnGlobalSearchActionListener listener) {
			super(itemView);

			listName = itemView.findViewById(R.id.list_name);
			postsScrollView = itemView.findViewById(R.id.scroll_view);
			postsContainer = itemView.findViewById(R.id.card_container);
			noResult = itemView.findViewById(R.id.no_result);

			mListener = listener;
		}

		void setPostList(final PostList list, final String returnType, int position) {
			if (list == null)
				return;

			returnTypeString = returnType;

			listName.setText(list.getNameToDisplay());

			if (postsScrollView != null) {
				postsScrollView.setTag(null);
				postsScrollView.setTag(position);
				mListener.saveScrollView(position, postsScrollView);

				List<Post> posts = list.getPosts();
				if (posts.isEmpty()) {
					postsScrollView.setVisibility(View.GONE);
					noResult.setText(R.string.no_posts_in_list);
					noResult.setVisibility(View.VISIBLE);
				} else {
					postsScrollView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					if (postsContainer != null) {
						postsContainer.removeAllViews();

						Context context = postsContainer.getContext();
						for (int i = 0; i < posts.size(); i++) {
							final Post p = posts.get(i);
							if (p != null && Utils.isContextValid(context)) {
								View v = LayoutInflater.from(context)
										.inflate(p.getRightDiaryLayoutItem(), postsContainer, false);

								if (v != null) {
									TextView tv = v.findViewById(R.id.text);
									if (p.isPicturePost() || p.isWebLinkPost()) {
										ImageView iv = v.findViewById(R.id.post_preview);
										if (iv != null) {
											if (Utils.hasLollipop())
												MediaHelper.loadPictureWithGlide(iv.getContext(), p.getContent(false), iv);
											else
												MediaHelper.roundPictureCorners(iv, p.getContent(false));
										}
									} else if (p.isVideoPost()) {
										ImageView iv = v.findViewById(R.id.video_view_thumbnail);
										if (iv != null) {
											if (Utils.hasLollipop())
												MediaHelper.loadPictureWithGlide(iv.getContext(), p.getVideoThumbnail(), iv);
											else
												MediaHelper.roundPictureCorners(iv, p.getVideoThumbnail());
										}
									}

									// show caption/message only on text posts preview
									if (p.isTextPost()) {
										tv.setText(p.getCaption());
										tv.setTextColor(MemoryColorEnum.getColor(mListener.getResources(), p.getTextColor()));
										if (v instanceof CardView)
											((CardView) v).setCardBackgroundColor(p.getBackgroundColor(mListener.getResources()));
									}


									if (i == 0) {
										LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
										lp.setMarginStart(Utils.dpToPx(20f, v.getResources()));
										v.setLayoutParams(lp);
									}

									v.setOnClickListener(view -> mListener.goToTimeline(returnTypeString, p.getId()));

									postsContainer.addView(v);
								}
							}
						}

						if (list.hasMoreData()) {
							View v = LayoutInflater.from(context)
									.inflate(R.layout.item_diary_more_post, postsContainer, false);

							if (v != null) {
								v.setOnClickListener(view -> mListener.goToTimeline(returnTypeString, null));

								LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
								lp.setMarginEnd(Utils.dpToPx(20f, v.getResources()));
								v.setLayoutParams(lp);

								postsContainer.addView(v);
							}
						}
					}
				}
			}

			mListener.restoreScrollView(position);
		}
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InterestCategory} or {@link com.groops.fairsquare.models.UsersBundle}.
	 */
	class InterestPeopleContainerVH extends RecyclerView.ViewHolder {

		private final View itemView;

		private final TextView listName;
		private final TextView noResult;
		private final GridLayout gridLayout;
		private final TextView viewMore;

		private final OnGlobalSearchActionListener mListener;

		private GlobalSearchTypeEnum objectType = null;
		private String name = "";


		InterestPeopleContainerVH(View itemView, OnGlobalSearchActionListener listener) {
			super(itemView);

			this.itemView = itemView;

			listName = itemView.findViewById(R.id.title);
			noResult = itemView.findViewById(R.id.no_result);

			gridLayout = itemView.findViewById(R.id.grid_layout);
			gridLayout.setColumnCount(3);
			viewMore = itemView.findViewById(R.id.view_more);

			mListener = listener;
		}

		void setItem(final Object list) {

			List<Object> objects = new ArrayList<>();
			boolean moreData = false;
			if (list instanceof InterestCategory) {
				name = ((InterestCategory) list).getNameToDisplay();
				objects.addAll(((InterestCategory) list).getInterests());
				moreData = ((InterestCategory) list).hasMoreData();
			}
			else if (list instanceof UsersBundle) {
				name = ((UsersBundle) list).getNameToDisplay();
				objects.addAll(((UsersBundle) list).getUsers());
				moreData = ((UsersBundle) list).hasMoreData();
			}

			if (Utils.isStringValid(name))
				listName.setText(name);

			gridLayout.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			gridLayout.removeAllViews();

			int rows = objects.size() % 3;
			gridLayout.setRowCount((rows > 0) ? (objects.size() / 3) + 1 : (objects.size() / 3));

			for (int i = 0; i < objects.size(); i++) {
				final Object obj = objects.get(i);

				if (obj instanceof Interest)
					setInterestItem(gridLayout.getContext(), ((Interest) obj));
				else if (obj instanceof HLUserGeneric)
					setUserItem(gridLayout.getContext(), ((HLUserGeneric) obj));
			}

			viewMore.setVisibility(View.GONE);
			viewMore.setText(Utils.getFormattedHtml(viewMore.getResources().getString(R.string.view_more)));
			viewMore.setOnClickListener(v -> {
                if (mListener != null && objectType != null)
                    mListener.goToInterestsUsersList(objectType, name);
            });

			if (moreData) {
				View v = LayoutInflater.from(gridLayout.getContext())
						.inflate(R.layout.item_interest_global_search_more, gridLayout, false);

				if (v != null) {
					v.setOnClickListener(view -> {
                        if (mListener != null && objectType != null)
                            mListener.goToInterestsUsersList(objectType, name);
                    });

//					LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
//					lp.setMarginEnd(Utils.dpToPx(20f, v.getResources()));
//					v.setLayoutParams(lp);

					gridLayout.addView(v);
				}
			}
			else if (rows > 0) {
				for (int j = 0; j < 3 - rows; j++) {
					setInvisibleItem(gridLayout.getContext());
				}
			}
		}

		private void setUserItem(Context context, @NonNull final HLUserGeneric user) {
			View userView = LayoutInflater.from(context).inflate(R.layout.item_user_global_search, gridLayout, false);
			if (userView != null) {
				ImageView pictureView = userView.findViewById(R.id.profile_picture);
				if (Utils.isStringValid(user.getAvatarURL()))
					MediaHelper.loadProfilePictureWithPlaceholder(context, user.getAvatarURL(), pictureView);
				else
					pictureView.setImageResource(R.drawable.ic_profile_placeholder);

				((TextView) userView.findViewById(R.id.user_name)).setText(user.getCompleteName());

				userView.setOnClickListener(v -> {
                    if (mListener != null)
                        mListener.goToInterestUserProfile(user.getId(), false);
                });

				gridLayout.addView(userView);
			}
		}

		private void setInterestItem(Context context, @NonNull final Interest interest) {
			View interestView = LayoutInflater.from(context).inflate(R.layout.item_interest_global_search, gridLayout, false);
			if (interestView != null) {
				ImageView iv = interestView.findViewById(R.id.interest_image);
				if (iv != null) {
					MediaHelper.roundPictureCorners(iv, interest.getAvatarURL());
				}

				// INFO: 2019-05-06    statically made GONE from layout
				((TextView) interestView.findViewById(R.id.interest_name)).setText(interest.getName());

				interestView.setOnClickListener(v -> {
                    if (mListener != null)
                        mListener.goToInterestUserProfile(interest.getId(), true);
                });

				gridLayout.addView(interestView);
			}
		}

		private void setInvisibleItem(Context context) {
			View interestView = LayoutInflater.from(context).inflate(R.layout.item_interest_global_search, gridLayout, false);
			if (interestView != null) {
				interestView.setVisibility(View.INVISIBLE);
				gridLayout.addView(interestView);
			}
		}

		void setObjectType(GlobalSearchTypeEnum objectType) {
			this.objectType = objectType;
		}

	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InterestCategory} or {@link com.groops.fairsquare.models.UsersBundle}
	 * shown in a {@link HorizontalScrollView}.
	 */
	static class InterestPeopleContainerScrollViewVH extends RecyclerView.ViewHolder {

		private final TextView listName;
		private final TextView noResult;

		private final HorizontalScrollView circlesScrollView;
		private final ViewGroup circlesContainer;

		private final OnGlobalSearchActionListener mListener;

		private GlobalSearchTypeEnum objectType = null;
		private String name = "";

		InterestPeopleContainerScrollViewVH(View itemView, OnGlobalSearchActionListener listener) {
			super(itemView);

			listName = itemView.findViewById(R.id.title);
			circlesScrollView = itemView.findViewById(R.id.scroll_view);
			circlesContainer = itemView.findViewById(R.id.items_container);
			noResult = itemView.findViewById(R.id.no_result);

			mListener = listener;
		}

		void setCirclesList(final Object list, int position) {
			if (list == null)
				return;

			List<Object> objects = new ArrayList<>();
			boolean moreData = false;
			if (list instanceof InterestCategory) {
				name = ((InterestCategory) list).getNameToDisplay();
				objects.addAll(((InterestCategory) list).getInterests());
				moreData = ((InterestCategory) list).hasMoreData();
			}
			else if (list instanceof UsersBundle) {
				name = ((UsersBundle) list).getNameToDisplay();
				objects.addAll(((UsersBundle) list).getUsers());
				moreData = ((UsersBundle) list).hasMoreData();
			}

			if (Utils.isStringValid(name))
				listName.setText(name);

			if (circlesScrollView != null) {
				circlesScrollView.setTag(null);
				circlesScrollView.setTag(position);
				mListener.saveScrollView(position, circlesScrollView);

				if (objects.isEmpty()) {
					circlesScrollView.setVisibility(View.GONE);
					noResult.setVisibility(View.VISIBLE);
				} else {
					circlesScrollView.setVisibility(View.VISIBLE);
					noResult.setVisibility(View.GONE);

					if (circlesContainer != null) {
						circlesContainer.removeAllViews();

						Context context = circlesContainer.getContext();
						for (int i = 0; i < objects.size(); i++) {
                            setItem(context, objects.get(i));
						}

						if (moreData) {
							View v = LayoutInflater.from(context)
									.inflate(R.layout.item_search_round_more, circlesContainer, false);

							if (v != null) {
								v.setOnClickListener(view ->
                                        {
                                            if (objectType != null)
                                                mListener.goToInterestsUsersList(objectType, name);
                                        }
                                    );

//								LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) v.getLayoutParams());
//								lp.setMarginEnd(Utils.dpToPx(20f, v.getResources()));
//								v.setLayoutParams(lp);

								circlesContainer.addView(v);
							}
						}
					}
				}
			}

			mListener.restoreScrollView(position);
		}

		private void setItem(Context context, @NonNull final Object obj) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_search_round, circlesContainer, false);
            if (view != null) {
                String id = null, avatar = null, name = null;
                boolean interest = false;
                if (obj instanceof HLUserGeneric) {
                    id = ((HLUserGeneric) obj).getId();
                    avatar = ((HLUserGeneric) obj).getAvatarURL();
                    name = ((HLUserGeneric) obj).getCompleteName();
                } else if (obj instanceof Interest) {
                    id = ((Interest) obj).getId();
                    avatar = ((Interest) obj).getAvatarURL();
                    name = ((Interest) obj).getName();
                    interest = true;
                }

                ImageView iv = view.findViewById(R.id.single_picture);
                if (iv != null) {
                    if (Utils.isStringValid(avatar))
                        MediaHelper.loadProfilePictureWithPlaceholder(context, avatar, iv);
                    else
                        iv.setImageResource(R.drawable.ic_profile_placeholder);
                }
                ((TextView) view.findViewById(R.id.name)).setText(name);

                final boolean finalIsInterest = interest;
                final String finalId = id;

                view.setOnClickListener(v -> {
                    if (mListener != null)
                        mListener.goToInterestUserProfile(finalId, finalIsInterest);
                });

                circlesContainer.addView(view);
            }
        }

        void setObjectType(GlobalSearchTypeEnum objectType) {
            this.objectType = objectType;
        }
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the {@link android.widget.TextView}
	 * holding the SHOP cta.
	 *
	 * @since 4/16/2019.
	 */
	static class ShopButtonVH extends RecyclerView.ViewHolder {

		private final OnGlobalSearchActionListener mListener;

		ShopButtonVH(View itemView, OnGlobalSearchActionListener listener) {
			super(itemView);

			mListener = listener;

			itemView.setOnClickListener(v -> mListener.goToShop());

			int margin = Utils.dpToPx(R.dimen.activity_margin_lg, itemView.getResources());

			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			lp.height = Utils.dpToPx(48f, itemView.getResources());
			lp.setMargins(margin, margin, margin, margin);
			itemView.setLayoutParams(lp);

			itemView.setBackgroundResource(R.drawable.selector_button_shop);
			((TextView) itemView).setIncludeFontPadding(false);
			((TextView) itemView).setGravity(Gravity.CENTER);
			((TextView) itemView).setTypeface(ResourcesCompat.getFont(itemView.getContext(), R.font.raleway_bold));
			((TextView) itemView).setTextColor(ResourcesCompat.getColorStateList(itemView.getResources(), R.color.state_list_login_inverted, null));
			((TextView) itemView).setTextSize(18f);
			((TextView) itemView).setText(R.string.menu_title_shop);
			((TextView) itemView).setAllCaps(false);
		}
	}


	public interface OnGlobalSearchActionListener {
		void goToTimeline(@NonNull String listName, @Nullable String postId);
		void goToInterestsUsersList(GlobalSearchTypeEnum returnType, String title);
		void goToInterestUserProfile(String id, boolean isInterest);
		void saveScrollView(int position, HorizontalScrollView scrollView);
		void restoreScrollView(int position);
		Resources getResources();

		void goToShop();
	}

}
