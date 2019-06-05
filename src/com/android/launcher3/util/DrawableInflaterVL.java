/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.VectorDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * Instantiates a drawable XML file into its corresponding
 * {@link Drawable} objects.
 * <p>
 * For performance reasons, inflation relies heavily on pre-processing of
 * XML files that is done at build time. Therefore, it is not currently possible
 * to use this inflater with an XmlPullParser over a plain XML file at runtime;
 * it only works with an XmlPullParser returned from a compiled resource (R.
 * <em>something</em> file.)
 *
 * @hide Pending API finalization.
 */
public final class DrawableInflaterVL {
    private static final HashMap<String, Constructor<? extends Drawable>> CONSTRUCTOR_MAP =
            new HashMap<>();

    private final Resources mRes;
    public ClassLoader mClassLoader;

    /**
     * Loads the drawable resource with the specified identifier.
     *
     * @param context the context in which the drawable should be loaded
     * @param id the identifier of the drawable resource
     * @return a drawable, or {@code null} if the drawable failed to load
     */
    @Nullable
    public static Drawable loadDrawable(@NonNull Context context, @DrawableRes int id) {
        return loadDrawable(context.getResources(), context.getTheme(), id);
    }

    /**
     * Loads the drawable resource with the specified identifier.
     *
     * @param resources the resources from which the drawable should be loaded
     * @param theme the theme against which the drawable should be inflated
     * @param id the identifier of the drawable resource
     * @return a drawable, or {@code null} if the drawable failed to load
     */
    @Nullable
    public static Drawable loadDrawable(
            @NonNull Resources resources, @Nullable Theme theme, @DrawableRes int id) {
        return resources.getDrawable(id, theme);
    }

    /**
     * Constructs a new drawable inflater using the specified resources and
     * class loader.
     *
     * @param res the resources used to resolve resource identifiers
     * @param classLoader the class loader used to load custom drawables
     * @hide
     */
    public DrawableInflaterVL(@NonNull Resources res, @NonNull ClassLoader classLoader) {
        mRes = res;
        mClassLoader = classLoader;
    }

    /**
     * Inflates a drawable from inside an XML document using an optional
     * {@link Theme}.
     * <p>
     * This method should be called on a parser positioned at a tag in an XML
     * document defining a drawable resource. It will attempt to create a
     * Drawable from the tag at the current position.
     *
     * @param name the name of the tag at the current position
     * @param parser an XML parser positioned at the drawable tag
     * @param attrs an attribute set that wraps the parser
     * @param theme the theme against which the drawable should be inflated, or
     *              {@code null} to not inflate against a theme
     * @return a drawable
     *
     * @throws XmlPullParserException
     * @throws IOException
     */
    @NonNull
    public Drawable inflateFromXml(@NonNull String name, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        // Inner classes must be referenced as Outer$Inner, but XML tag names
        // can't contain $, so the <drawable> tag allows developers to specify
        // the class in an attribute. We'll still run it through inflateFromTag
        // to stay consistent with how LayoutInflater works.
        if (name.equals("drawable")) {
            name = attrs.getAttributeValue(null, "class");
            if (name == null) {
                throw new InflateException("<drawable> tag must specify class attribute");
            }
        }

        Drawable drawable = inflateFromTag(name);
        if (drawable == null) {
            drawable = inflateFromClass(name);
        }
        drawable.inflate(mRes, parser, attrs, theme);
        return drawable;
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private Drawable inflateFromTag(@NonNull String name) {
        switch (name) {
            case "selector":
                return new StateListDrawable();
            case "animated-selector":
                return new AnimatedStateListDrawable();
            case "level-list":
                return new LevelListDrawable();
            case "color":
                return new ColorDrawable();
            case "shape":
                return new GradientDrawable();
            case "vector":
                return new VectorDrawable();
            case "animated-vector":
                return new AnimatedVectorDrawable();
            case "rotate":
                return new RotateDrawable();
            case "animation-list":
                return new AnimationDrawable();
            case "bitmap":
                return new BitmapDrawable();
            case "inset":
                return getPrivateConstructor(InsetDrawable.class);
            case "layer-list":
                return getPrivateConstructor(LayerDrawable.class);
            case "transition":
                return getPrivateConstructor(TransitionDrawable.class);
            case "ripple":
                return getPrivateConstructor(RippleDrawable.class);
            case "scale":
                return getPrivateConstructor(ScaleDrawable.class);
            case "clip":
                return getPrivateConstructor(ClipDrawable.class);
            case "nine-patch":
                return getPrivateConstructor(NinePatchDrawable.class);
            default:
                return null;
        }
    }

    public Drawable getPrivateConstructor(Class c) {
        try {Constructor constructor = c.getConstructor();
        constructor.setAccessible(true);
        return (Drawable) (constructor.newInstance());}
        catch (Exception e){
            Log.e("Reflection error: ","error accessing drawable constructor: ",e); return null;}
    }

    @NonNull
    private Drawable inflateFromClass(@NonNull String className) {
        try {
            Constructor<? extends Drawable> constructor;
            synchronized (CONSTRUCTOR_MAP) {
                constructor = CONSTRUCTOR_MAP.get(className);
                if (constructor == null) {
                    final Class<? extends Drawable> clazz =
                            mClassLoader.loadClass(className).asSubclass(Drawable.class);
                    constructor = clazz.getConstructor();
                    CONSTRUCTOR_MAP.put(className, constructor);
                }
            }
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            final InflateException ie = new InflateException(
                    "Error inflating class " + className);
            ie.initCause(e);
            throw ie;
        } catch (ClassCastException e) {
            // If loaded class is not a Drawable subclass.
            final InflateException ie = new InflateException(
                    "Class is not a Drawable " + className);
            ie.initCause(e);
            throw ie;
        } catch (ClassNotFoundException e) {
            // If loadClass fails, we should propagate the exception.
            final InflateException ie = new InflateException(
                    "Class not found " + className);
            ie.initCause(e);
            throw ie;
        } catch (Exception e) {
            final InflateException ie = new InflateException(
                    "Error inflating class " + className);
            ie.initCause(e);
            throw ie;
        }
    }
}