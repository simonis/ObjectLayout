/*
 * Written by Gil Tene and Martin Thompson, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.ObjectLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link IntrinsicObjectModel} models the information needed to make a specific instance field of a
 * containing class intrinsic to the instances of that containing class. Intrinsic objects may have their
 * layout within the containing object instance optimized by JDK implementations, such that access to
 * their content is faster, and avoids certain de-referencing steps.
 *
 * @param <T> The type of the intrinsic object
 */
final class IntrinsicObjectModel<T> extends AbstractIntrinsicObjectModel<T> {

    IntrinsicObjectModel(
            Field field,
            final PrimitiveArrayModel primitiveArrayModel,
            final StructuredArrayModel structuredArrayModel) {
        super(field, primitiveArrayModel, structuredArrayModel);
    }

    /**
     * Construct an intrinsic object within the containing object, using a default constructor.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @return A reference to the the newly constructed intrinsic object
     */
    final T constructWithin(final Object containingObject) {
        try {
            return instantiate(
                    containingObject,
                    getObjectClass().getDeclaredConstructor(),
                    (Object[]) null
            );
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Construct an intrinsic object within the containing object, using the given constructor and arguments.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param objectConstructor The constructor to be used in constructing the intrinsic object instance
     * @param args the arguments to be used with the objectConstructor
     * @return A reference to the the newly constructed intrinsic object
     */
    final T constructWithin(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {
        if (getObjectClass() != objectConstructor.getDeclaringClass()) {
            throw new IllegalArgumentException(
                    "The declaring class of the constructor (" +
                            objectConstructor.getDeclaringClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
                            ")");
        }
        return instantiate(
                containingObject,
                objectConstructor,
                args
        );
    }

    /**
     * Construct an intrinsic object within the containing object, using the constructor and arguments
     * supplied in the given objectCtorAndArgs argument.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param objectCtorAndArgs The constructor and arguments to be used in constructing the
     *                          intrinsic object instance
     * @return A reference to the the newly constructed intrinsic object
     */
    final T constructWithin(
            final Object containingObject,
            final CtorAndArgs<T> objectCtorAndArgs) {
        if (getObjectClass() != objectCtorAndArgs.getConstructor().getDeclaringClass()) {
            throw new IllegalArgumentException(
                    "The declaring class of the constructor (" +
                            objectCtorAndArgs.getConstructor().getDeclaringClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
                            ")");
        }
        return instantiate(
                containingObject,
                objectCtorAndArgs.getConstructor(),
                objectCtorAndArgs.getArgs()
        );
    }

    /**
     * Construct an intrinsic object within the containing object, using the supplied {@link StructuredArrayBuilder}.
     * This form of constructWithin() can only be used to construct intrinsic objects that derive from
     * StructuredArray.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param arrayBuilder The {@link StructuredArrayBuilder} instance to be used in constructing the array
     * @return A reference to the the newly constructed intrinsic object
     */
    final T constructWithin(
            final Object containingObject,
            final StructuredArrayBuilder arrayBuilder) {
        if (!_isStructuredArray()) {
            throw new IllegalArgumentException(
                    "The StructuredArrayBuilder argument cannot be used on IntrinsicObjectModel" +
                            " that do not model StructuredArray intrinsic objects"
            );
        }
        if (getObjectClass() != arrayBuilder.getArrayModel().getArrayClass()) {
            throw new IllegalArgumentException(
                    "The class in the array builder (" +
                            arrayBuilder.getArrayModel().getArrayClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
                            ")");
        }
        if (!arrayBuilder.getArrayModel().equals(_getStructuredArrayModel())) {
            throw new IllegalArgumentException(
                    "The array model in supplied array builder does not match the array model used" +
                            " to create this IntrinsicObjectModel instance."
            );
        }
        return instantiate(containingObject, arrayBuilder);
    }

    /**
     * Construct an intrinsic object within the containing object, using the supplied {@link PrimitiveArrayBuilder}.
     * This form of constructWithin() can only be used to construct intrinsic objects that derive from
     * StructuredArray.
     *
     * @param containingObject The object instance that will contain this intrinsic object
     * @param arrayBuilder The {@link PrimitiveArrayBuilder} instance to be used in constructing the array
     * @return A reference to the the newly constructed intrinsic object
     */
    final T constructWithin(
            final Object containingObject,
            final PrimitiveArrayBuilder arrayBuilder) {
        if (!_isPrimitiveArray()) {
            throw new IllegalArgumentException(
                    "The PrimitiveArrayBuilder argument cannot be used on IntrinsicObjectModel" +
                            " that do not model PrimitiveArrayBuilder intrinsic objects"
            );
        }
        if (getObjectClass() != arrayBuilder.getArrayModel().getArrayClass()) {
            throw new IllegalArgumentException(
                    "The class in the array builder (" +
                            arrayBuilder.getArrayModel().getArrayClass().getName() +
                            ") does not match the intrinsic object class declared in the model (" +
                            getObjectClass().getName() +
                            ")");
        }
        if (!arrayBuilder.getArrayModel().equals(_getPrimitiveArrayModel())) {
            throw new IllegalArgumentException(
                    "The array model in supplied array builder does not match the array model used" +
                            " to create this IntrinsicObjectModel instance."
            );
        }
        return instantiate(containingObject, arrayBuilder);
    }

    static final ThreadLocal<Set<IntrinsicObjectModel<?>>> modelSet =
            new ThreadLocal<Set<IntrinsicObjectModel<?>>> () {
                @Override
                protected Set<IntrinsicObjectModel<?>> initialValue() {
                    return new HashSet<IntrinsicObjectModel<?>>();
                }
    };

    private void circularityCheckEnter() {
        if (modelSet.get().contains(this)) {
            modelSet.get().clear();
            throw new ClassCircularityError("Recursively intrinsifying the field: " + this.getField());
        }
        else {
            modelSet.get().add(this);
        }
    }

    private void circularityCheckExit() {
        modelSet.get().remove(this);
    }

    private void handleCircularityError(InvocationTargetException ex) {
        if (ex.getCause() != null && ex.getCause() instanceof ClassCircularityError) {
            throw (ClassCircularityError)ex.getCause();
        } else {
            throw new RuntimeException(ex);
        }
    }

    private <E> T instantiate(
            final Object containingObject,
            final Constructor<T> objectConstructor,
            final Object... args) {

        circularityCheckEnter();
        try {
            _sanityCheckInstantiation(containingObject);
            T intrinsicInstance;
            if (_isPrimitiveArray()) {
                intrinsicInstance =
                        constructPrimitiveArrayWithin(containingObject, objectConstructor, args);
            } else if (_isStructuredArray()) {
                @SuppressWarnings("unchecked")
                StructuredArrayBuilder<StructuredArray<E>, E> arrayBuilder =
                        new StructuredArrayBuilder((StructuredArrayModel)_getStructuredArrayModel()).
                                arrayCtorAndArgs(objectConstructor, args).resolve();
                @SuppressWarnings("unchecked")
                IntrinsicObjectModel<StructuredArray<E>> model =
                        (IntrinsicObjectModel<StructuredArray<E>>) this;
                @SuppressWarnings("unchecked")
                T array = (T) StructuredArray.constructStructuredArrayWithin(containingObject, model, arrayBuilder);
                intrinsicInstance = array;
            } else {
                intrinsicInstance =
                        constructElementWithin(containingObject, objectConstructor, args);
            }
            return intrinsicInstance;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            handleCircularityError(ex);
            // THe following throw will never be executed because handleCircularityError() either throws
            // a RuntimeException or a ClassCircualrityError. We just need it to calm down the compiler.
            throw new RuntimeException(ex);
        } finally {
            circularityCheckExit();
        }
    }

    private <E> T instantiate(
            final Object containingObject,
            final StructuredArrayBuilder arrayBuilder) {
        circularityCheckEnter();
        try {
            _sanityCheckInstantiation(containingObject);
            @SuppressWarnings("unchecked")
            StructuredArrayBuilder<StructuredArray<E>, E> builder = arrayBuilder;
            @SuppressWarnings("unchecked")
            IntrinsicObjectModel<StructuredArray<E>> model =
                    (IntrinsicObjectModel<StructuredArray<E>>) this;
            @SuppressWarnings("unchecked")
            T intrinsicInstance =
                    (T) StructuredArray.constructStructuredArrayWithin(containingObject, model, builder);
            return intrinsicInstance;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            handleCircularityError(ex);
            // THe following throw will never be executed because handleCircularityError() either throws
            // a RuntimeException or a ClassCircualrityError. We just need it to calm down the compiler.
            throw new RuntimeException(ex);
        } finally {
            circularityCheckExit();
        }
    }

    private T instantiate(
            final Object containingObject,
            final PrimitiveArrayBuilder arrayBuilder) {
        circularityCheckEnter();
        try {
            _sanityCheckInstantiation(containingObject);
            @SuppressWarnings("unchecked")
            PrimitiveArrayBuilder<AbstractPrimitiveArray> builder = arrayBuilder;
            @SuppressWarnings("unchecked")
            IntrinsicObjectModel<AbstractPrimitiveArray> model = (IntrinsicObjectModel<AbstractPrimitiveArray>) this;
            @SuppressWarnings("unchecked")
            T intrinsicInstance =
                    (T) PrimitiveArrayBuilder.constructPrimitiveArrayWithin(containingObject, model, builder);
            return intrinsicInstance;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            handleCircularityError(ex);
            // THe following throw will never be executed because handleCircularityError() either throws
            // a RuntimeException or a ClassCircualrityError. We just need it to calm down the compiler.
            throw new RuntimeException(ex);
        } finally {
            circularityCheckExit();
        }
    }
}
