/*
// $Id$
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2002-2002 Kana Software, Inc.
// Copyright (C) 2002-2009 Julian Hyde and others
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
//
// jhyde, 3 March, 2002
*/
package mondrian.olap.fun;

import mondrian.olap.*;
import mondrian.olap.type.*;
import mondrian.resource.MondrianResource;
import mondrian.calc.*;
import mondrian.mdx.*;
import mondrian.rolap.RolapHierarchy;
import mondrian.util.FilteredIterableList;
import mondrian.util.ConcatenableList;
import mondrian.util.Pair;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.log4j.Logger;
import org.apache.commons.collections.comparators.*;

import java.util.*;

/**
 * {@code FunUtil} contains a set of methods useful within the {@code
 * mondrian.olap.fun} package.
 *
 * @author jhyde
 * @version $Id$
 * @since 1.0
 */
public class FunUtil extends Util {

    static final String[] emptyStringArray = new String[0];
    private static final boolean debug = false;
    public static final NullMember NullMember = new NullMember();

    /**
     * Special value which indicates that a {@code double} computation
     * has returned the MDX null value. See {@link DoubleCalc}.
     */
    public static final double DoubleNull = 0.000000012345;

    /**
     * Special value which indicates that a {@code double} computation
     * has returned the MDX EMPTY value. See {@link DoubleCalc}.
     */
    public static final double DoubleEmpty = -0.000000012345;

    /**
     * Special value which indicates that an {@code int} computation
     * has returned the MDX null value. See {@link mondrian.calc.IntegerCalc}.
     */
    public static final int IntegerNull = Integer.MIN_VALUE + 1;

    /**
     * Null value in three-valued boolean logic.
     * Actually, a placeholder until we actually implement 3VL.
     */
    public static final boolean BooleanNull = false;

    /**
     * Creates an exception which indicates that an error has occurred while
     * executing a given function.
     */
    public static RuntimeException newEvalException(
        FunDef funDef,
        String message)
    {
        Util.discard(funDef); // TODO: use this
        return new MondrianEvaluationException(message);
    }

    /**
     * Creates an exception which indicates that an error has occurred while
     * executing a given function.
     */
    public static RuntimeException newEvalException(Throwable throwable) {
        return new MondrianEvaluationException(
            throwable.getClass().getName() + ": " + throwable.getMessage());
    }

    public static boolean isMemberType(Calc calc) {
        Type type = calc.getType();
        return (type instanceof SetType)
            && (((SetType) type).getArity() == 1);
    }

    public static void checkIterListResultStyles(Calc calc) {
        switch (calc.getResultStyle()) {
        case ITERABLE:
        case LIST:
        case MUTABLE_LIST:
            break;
        default:
            throw ResultStyleException.generateBadType(
                ResultStyle.ITERABLE_LIST_MUTABLELIST,
                calc.getResultStyle());
        }
    }

    public static void checkListResultStyles(Calc calc) {
        switch (calc.getResultStyle()) {
        case LIST:
        case MUTABLE_LIST:
            break;
        default:
            throw ResultStyleException.generateBadType(
                ResultStyle.LIST_MUTABLELIST,
                calc.getResultStyle());
        }
    }

    /**
     * Returns an argument whose value is a literal.
     */
    static String getLiteralArg(
        ResolvedFunCall call,
        int i,
        String defaultValue,
        String[] allowedValues)
    {
        if (i >= call.getArgCount()) {
            if (defaultValue == null) {
                throw newEvalException(
                    call.getFunDef(),
                    "Required argument is missing");
            } else {
                return defaultValue;
            }
        }
        Exp arg = call.getArg(i);
        if (!(arg instanceof Literal)
            || arg.getCategory() != Category.Symbol)
        {
            throw newEvalException(
                call.getFunDef(),
                "Expected a symbol, found '" + arg + "'");
        }
        String s = (String) ((Literal) arg).getValue();
        StringBuilder sb = new StringBuilder(64);
        for (int j = 0; j < allowedValues.length; j++) {
            String allowedValue = allowedValues[j];
            if (allowedValue.equalsIgnoreCase(s)) {
                return allowedValue;
            }
            if (j > 0) {
                sb.append(", ");
            }
            sb.append(allowedValue);
        }
        throw newEvalException(
            call.getFunDef(),
            "Allowed values are: {" + sb + "}");
    }

    /**
     * Returns the ordinal of a literal argument. If the argument does not
     * belong to the supplied enumeration, returns -1.
     */
    static <E extends Enum<E>> E getLiteralArg(
        ResolvedFunCall call,
        int i,
        E defaultValue,
        Class<E> allowedValues)
    {
        if (i >= call.getArgCount()) {
            if (defaultValue == null) {
                throw newEvalException(
                    call.getFunDef(),
                    "Required argument is missing");
            } else {
                return defaultValue;
            }
        }
        Exp arg = call.getArg(i);
        if (!(arg instanceof Literal)
            || arg.getCategory() != Category.Symbol)
        {
            throw newEvalException(
                call.getFunDef(),
                "Expected a symbol, found '" + arg + "'");
        }
        String s = (String) ((Literal) arg).getValue();
        for (E e : allowedValues.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(s)) {
                return e;
            }
        }
        StringBuilder buf = new StringBuilder(64);
        int k = 0;
        for (E e : allowedValues.getEnumConstants()) {
            if (k++ > 0) {
                buf.append(", ");
            }
            buf.append(e.name());
        }
        throw newEvalException(
            call.getFunDef(),
            "Allowed values are: {" + buf + "}");
    }

    /**
     * Throws an error if the expressions don't have the same hierarchy.
     * @param left
     * @param right
     * @throws MondrianEvaluationException if expressions don't have the same
     *     hierarchy
     */
    static void checkCompatible(Exp left, Exp right, FunDef funDef) {
        final Type leftType = TypeUtil.stripSetType(left.getType());
        final Type rightType = TypeUtil.stripSetType(right.getType());
        if (!TypeUtil.isUnionCompatible(leftType, rightType)) {
            throw newEvalException(
                funDef, "Expressions must have the same hierarchy");
        }
    }

    /**
     * Returns {@code true} if the mask in {@code flag} is set.
     *
     * @param value The value to check.
     * @param mask The mask within value to look for.
     * @param strict If {@code true} all the bits in mask must be set. If
     * {@code false} the method will return {@code true} if any of the
     * bits in {@code mask} are set.
     * @return whether if the correct bits in {@code mask} are set.
     */
    static boolean checkFlag(int value, int mask, boolean strict) {
        return (strict)
            ? ((value & mask) == mask)
            : ((value & mask) != 0);
    }

    /**
     * Adds every element of {@code right} which is not in {@code set}
     * to both {@code set} and {@code left}.
     */
    static <T> void addUnique(List<T> left, List<T> right, Set<Object> set) {
        assert left != null;
        assert right != null;
        if (right.isEmpty()) {
            return;
        }
        for (int i = 0, n = right.size(); i < n; i++) {
            T o = right.get(i);
            Object p = o;
            if (o instanceof Object[]) {
                p = new ArrayHolder<Object>((Object[]) o);
            }
            if (set.add(p)) {
                left.add(o);
            }
        }
    }

    /**
     * Returns the default hierarchy of a dimension, or null if there is no
     * default.
     *
     * @see MondrianResource#CannotImplicitlyConvertDimensionToHierarchy
     *
     * @param dimension Dimension
     * @return Default hierarchy, or null
     */
    public static Hierarchy getDimensionDefaultHierarchy(Dimension dimension) {
        final Hierarchy[] hierarchies = dimension.getHierarchies();
        if (hierarchies.length == 1) {
            return hierarchies[0];
        }
        if (MondrianProperties.instance().SsasCompatibleNaming.get()) {
            // In SSAS 2005, dimensions with more than one hierarchy do not have
            // a default hierarchy.
            return null;
        }
        for (Hierarchy hierarchy : hierarchies) {
            if (hierarchy.getName() == null
                || hierarchy.getUniqueName().equals(dimension.getUniqueName()))
            {
                return hierarchy;
            }
        }
        return null;
    }

    static List<Member> addMembers(
        final SchemaReader schemaReader,
        final List<Member> members,
        final Hierarchy hierarchy)
    {
        // only add accessible levels
        for (Level level : schemaReader.getHierarchyLevels(hierarchy)) {
            addMembers(schemaReader, members, level);
        }
        return members;
    }

    static List<Member> addMembers(
        SchemaReader schemaReader,
        List<Member> members,
        Level level)
    {
        List<Member> levelMembers = schemaReader.getLevelMembers(level, true);
        members.addAll(levelMembers);
        return members;
    }

    /**
     * Removes every member from a list which is calculated.
     * The list must not be null, and must consist only of members.
     *
     * @param memberList Member list
     * @return List of non-calculated members
     */
    static List<Member> removeCalculatedMembers(List<Member> memberList)
    {
        return new FilteredIterableList<Member>(
            memberList,
            new FilteredIterableList.Filter<Member>() {
                public boolean accept(final Member m) {
                    return ! m.isCalculated();
                }
            }
        );
    }

    /**
     * Returns whether {@code m0} is an ancestor of {@code m1}.
     *
     * @param strict if true, a member is not an ancestor of itself
     */
    static boolean isAncestorOf(Member m0, Member m1, boolean strict) {
        if (strict) {
            if (m1 == null) {
                return false;
            }
            m1 = m1.getParentMember();
        }
        while (m1 != null) {
            if (m1.equals(m0)) {
                return true;
            }
            m1 = m1.getParentMember();
        }
        return false;
    }

    /**
     * For each member in a list, evaluates an expression and creates a map
     * from members to values.
     *
     * <p>If the list contains tuples, use
     * {@link #evaluateTuples(mondrian.olap.Evaluator, mondrian.calc.Calc, java.util.List)}.
     *
     * @param evaluator Evaluation context
     * @param exp Expression to evaluate
     * @param memberIter Iterable over the collection of members
     * @param memberList List to be populated with members, or null
     * @param parentsToo If true, evaluate the expression for all ancestors
     *            of the members as well
     *
     * @pre exp != null
     * @pre exp.getType() instanceof ScalarType
     */
    static Map<Member, Object> evaluateMembers(
        Evaluator evaluator,
        Calc exp,
        Iterable<Member> memberIter,
        List<Member> memberList,
        boolean parentsToo)
    {
        // REVIEW: is this necessary?
        evaluator = evaluator.push();

        assert exp.getType() instanceof ScalarType;
        Map<Member, Object> mapMemberToValue = new HashMap<Member, Object>();
        for (Member member : memberIter) {
            if (memberList != null) {
                memberList.add(member);
            }
            while (true) {
                evaluator.setContext(member);
                Object result = exp.evaluate(evaluator);
                if (result == null) {
                    result = Util.nullValue;
                }
                mapMemberToValue.put(member, result);
                if (!parentsToo) {
                    break;
                }
                member = member.getParentMember();
                if (member == null) {
                    break;
                }
                if (mapMemberToValue.containsKey(member)) {
                    break;
                }
            }
        }
        return mapMemberToValue;
    }

    /**
     * For each tuple in a list, evaluates an expression and creates a map
     * from tuples to values.
     *
     * @param evaluator Evaluation context
     * @param exp Expression to evaluate
     * @param members List of members (or List of Member[] tuples)
     *
     * @pre exp != null
     * @pre exp.getType() instanceof ScalarType
     */
    static Map<Object, Object> evaluateTuples(
        Evaluator evaluator,
        Calc exp,
        List<Member[]> members)
    {
        evaluator = evaluator.push();

        assert exp.getType() instanceof ScalarType;
        Map<Object, Object> mapMemberToValue = new HashMap<Object, Object>();
        for (int i = 0, count = members.size(); i < count; i++) {
            Member[] tuples = members.get(i);
            evaluator.setContext(tuples);
            Object result = exp.evaluate(evaluator);
            if (result == null) {
                result = Util.nullValue;
            }
            mapMemberToValue.put(new ArrayHolder<Member>(tuples), result);
        }
        return mapMemberToValue;
    }

    /**
     * Helper function to sort a list of members according to an expression.
     *
     * <p>NOTE: This function does not preserve the contents of the validator.
     *
     * <p>If you do not specify {@code memberList}, the method
     * will build its own member list as it iterates over {@code memberIter}.
     * It is acceptable if {@code memberList} and {@code memberIter} are the
     * same list object.
     *
     * <p>If you specify {@code memberList}, the list is sorted in place, and
     * memberList is returned.
     *
     * @param evaluator Evaluator
     * @param memberIter Iterable over members
     * @param memberList List of members
     * @param exp Expression to sort on
     * @param desc Whether to sort descending
     * @param brk Whether to break
     * @return sorted list (never null)
     */
    static List<Member> sortMembers(
        Evaluator evaluator,
        Iterable<Member> memberIter,
        List<Member> memberList,
        Calc exp,
        boolean desc,
        boolean brk)
    {
        if ((memberList != null) && (memberList.size() <= 1)) {
            return memberList;
        }

        // REVIEW mberkowitz 1/09: test whether precomputing values saves time.
        Map<Member, Object> mapMemberToValue;
        final boolean parentsToo = !brk;
        if (memberList == null) {
            memberList = new ArrayList<Member>();
            mapMemberToValue = evaluateMembers(
                evaluator, exp, memberIter, memberList, parentsToo);
        } else {
            mapMemberToValue = evaluateMembers(
                evaluator, exp, memberIter, null, parentsToo);
        }

        MemberComparator comp;
        if (brk) {
            comp = new BreakMemberComparator(evaluator, exp, desc);
        } else {
            comp = new HierarchicalMemberComparator(evaluator, exp, desc);
        }
        comp.preloadValues(mapMemberToValue);
        Collections.sort(memberList, comp.wrap());
        return memberList;
    }

    /**
     * Sorts a list of members according to a list of SortKeySpecs.
     * An in-place, Stable sort.
     * Helper function for MDX OrderSet function.
     *
     * <p>NOTE: Does not preserve the contents of the validator.
     */
    static List<Member> sortMembers(
        Evaluator evaluator,
        Iterable<Member> memberIter,
        List<Member> memberList,
        List<SortKeySpec> keySpecList)
    {
        if ((memberList != null) && (memberList.size() <= 1)) {
            return memberList;
        }
        if (memberList == null) {
            memberList = new ArrayList<Member>();
            for (Member member : memberIter) {
                memberList.add(member);
            }
            if (memberList.size() <= 1) {
                return memberList;
            }
        }

        ComparatorChain chain = new ComparatorChain();
        for (SortKeySpec key : keySpecList) {
            boolean brk = key.direction.brk;
            MemberComparator comp;
            if (brk) {
                comp = new BreakMemberComparator(
                    evaluator, key.key, key.direction.descending);
            } else {
                comp = new HierarchicalMemberComparator(
                    evaluator, key.key, key.direction.descending);
            }
            comp.preloadValues(memberList);
            chain.addComparator(comp.wrap(), false);
        }

        Collections.sort(memberList, chain);
        return memberList;
    }

    /**
     * Sorts a list of Tuples by the value of an applied expression. Stable
     * sort.
     *
     * <p>Helper function for MDX functions TopCount, TopSum, TopPercent,
     * BottomCount, BottomSum, BottomPercent, but not the MDX function Order.
     *
     * <p>NOTE: This function does not preserve the contents of the validator.
     *
     * <p>If you specify {@code tupleList}, the list is sorted in place, and
     * tupleList is returned.
     *
     * @param evaluator Evaluator
     * @param tupleIter Iterator over tuples
     * @param tupleList List of tuples, if known, otherwise null
     * @param exp Expression to sort on
     * @param desc Whether to sort descending
     * @param brk Whether to break
     * @param arity Number of members in each tuple
     * @return sorted list (never null)
     */
    public static List<Member[]> sortTuples(
        Evaluator evaluator,
        Iterable<Member[]> tupleIter,
        List<Member[]> tupleList,
        Calc exp,
        boolean desc,
        boolean brk,
        int arity)
    {
        // NOTE: This method does not implement the iterable/list concept
        // as fully as sortMembers. This is because sortMembers evaluates all
        // sort expressions up front. There, it is efficient to unravel the
        // iterator and evaluate the sort expressions at the same time.
        if (tupleList == null) {
            tupleList = new ArrayList<Member[]>();
            for (Member[] tuple : tupleIter) {
                tupleList.add(tuple);
            }
        }
        if (tupleList.size() <= 1) {
            return tupleList;
        }

        Comparator<Member[]> comparator;
        if (brk) {
            BreakArrayComparator c =
                new BreakArrayComparator(evaluator, exp, arity);
            c.preloadValues(tupleList);
            comparator = c.wrap();
            if (desc) {
                comparator = new ReverseComparator<Member[]>(comparator);
            }
        } else {
            comparator =
                new HierarchicalArrayComparator(evaluator, exp, arity, desc)
                .wrap();
        }

        Collections.sort(tupleList, comparator);
        return tupleList;
    }

    /**
     * Partially sorts a list of Members by the value of an applied expression.
     *
     * <p>Avoids sorting the whole list, finds only the <i>n</i>top (or bottom)
     * valued Members, and returns them as a new List. Helper function for MDX
     * functions TopCount and BottomCount.
     *
     * @param list a list of members
     * @param exp a Calc applied to each member to find its sort-key
     * @param evaluator
     * @param limit maximum count of members to return.
     * @param desc true to sort descending (and find TopCount), false to sort
     *   ascending (and find BottomCount).
     * @return the top or bottom members, as a new list.
     * <p>NOTE: Does not preserve the contents of the validator.
     */
    public static  List<Member> partiallySortMembers(
        Evaluator evaluator,
        List<Member> list,
        Calc exp,
        int limit,
        boolean desc)
    {
        MemberComparator comp = new BreakMemberComparator(evaluator, exp, desc);
        Map<Member, Object> valueMap =
            evaluateMembers(evaluator, exp, list, null, false);
        comp.preloadValues(valueMap);
        return stablePartialSort(list, comp.wrap(), limit);
    }

    /**
     * Helper function to sort a list of tuples according to a list
     * of expressions and a list of sorting flags.
     *
     * <p>NOTE: This function does not preserve the contents of the validator.
     */
    static List<Member[]> sortTuples(
        Evaluator evaluator,
        Iterable<Member[]> tupleIter,
        List<Member[]> tupleList,
        List<SortKeySpec> keySpecList,
        int arity)
    {
        if (tupleList == null) {
            tupleList = new ArrayList<Member[]>();
            for (Member[] tuple : tupleIter) {
                tupleList.add(tuple);
            }
        }
        if (tupleList.size() <= 1) {
            return tupleList;
        }

        ComparatorChain chain = new ComparatorChain();
        for (SortKeySpec key : keySpecList) {
            boolean brk = key.direction.brk;
            boolean orderByKey =
                key.key instanceof MemberOrderKeyFunDef.CalcImpl;
            if (brk) {
                ArrayExpMemoComparator comp =
                    new BreakArrayComparator(evaluator, key.key, arity);
                comp.preloadValues(tupleList);
                chain.addComparator(comp.wrap(), key.direction.descending);
            } else if (orderByKey) {
                ArrayExpMemoComparator comp =
                    new HierarchicalArrayKeyComparator(
                        evaluator, key.key, arity);
                comp.preloadValues(tupleList);
                chain.addComparator(comp.wrap(), key.direction.descending);
            } else {
                ArrayExpComparator comp =
                    new HierarchicalArrayComparator(
                        evaluator, key.key, arity, key.direction.descending);
                chain.addComparator(comp.wrap(), false);
            }
        }

        Collections.sort(tupleList, chain);
        return tupleList;
    }

    /**
     * Partially sorts a list of Tuples by the value of an applied expression.
     *
     * <p>Avoids sorting the whole list, finds only the <i>n</i> top (or bottom)
     * valued Tuples, and returns them as a new List. Helper function for MDX
     * functions TopCount and BottomCount.
     *
     * @param list a list of tuples
     * @param exp a Calc applied to each tple to find its sort-key
     * @param evaluator
     * @param limit maximum count of tuples to return.
     * @param desc true to sort descending (and find TopCount),
     *  false to sort ascending (and find BottomCount).
     * @return the top or bottom tuples, as a new list.
     * <p>NOTE: Does not preserve the contents of the validator.
     */
    public static List<Member[]> partiallySortTuples(
        Evaluator evaluator,
        List<Member[]> list,
        Calc exp,
        int limit,
        boolean desc,
        int arity)
    {
        Comparator<Member[]> comp =
            new BreakArrayComparator(evaluator, exp, arity).wrap();
        if (desc) {
            comp = new ReverseComparator<Member[]>(comp);
        }
        return stablePartialSort(list, comp, limit);
    }

    /**
     * Sorts a list of members into hierarchical order. The members must belong
     * to the same dimension.
     *
     * @param memberList List of members
     * @param post Whether to sort in post order; if false, sorts in pre order
     *
     * @see #hierarchizeTupleList(java.util.List, boolean, int)
     */
    public static void hierarchizeMemberList(
        List<Member> memberList,
        boolean post)
    {
        if (memberList.isEmpty()) {
            return;
        }
        if (memberList.get(0).getDimension().isHighCardinality()) {
            return;
        }
        Comparator<Member> comparator = new HierarchizeComparator(post);
        Collections.sort(memberList, comparator);
    }

    /**
     * Sorts a list of tuples into hierarchical order.
     *
     * @param tupleList List of tuples
     * @param post Whether to sort in post order; if false, sorts in pre order
     * @param arity Number of members in each tuple
     *
     * @see #hierarchizeMemberList(java.util.List, boolean)
     */
    public static void hierarchizeTupleList(
        List<Member[]> tupleList,
        boolean post,
        int arity)
    {
        if (tupleList.isEmpty()) {
            return;
        }
        Comparator<Member[]> comparator =
            new HierarchizeArrayComparator(arity, post).wrap();
        Collections.sort(tupleList, comparator);
    }

    static int sign(double d) {
        return (d == 0)
            ? 0
            : (d < 0)
                ? -1
                : 1;
    }

    /**
     * Compares double-precision values according to MDX semantics.
     *
     * <p>MDX requires a total order:
     * <blockquote>
     *    -inf &lt; NULL &lt; ... &lt; -1 &lt; ... &lt; 0 &lt; ... &lt; NaN &lt;
     * +inf
     * </blockquote>
     * but this is different than Java semantics, specifically with regard
     * to {@link Double#NaN}.
     */
    public static int compareValues(double d1, double d2) {
        if (Double.isNaN(d1)) {
            if (d2 == Double.POSITIVE_INFINITY) {
                return -1;
            } else if (Double.isNaN(d2)) {
                return 0;
            } else {
                return 1;
            }
        } else if (Double.isNaN(d2)) {
            if (d1 == Double.POSITIVE_INFINITY) {
                return 1;
            } else {
                return -1;
            }
        } else if (d1 == d2) {
            return 0;
        } else if (d1 == FunUtil.DoubleNull) {
            if (d2 == Double.NEGATIVE_INFINITY) {
                return 1;
            } else {
                return -1;
            }
        } else if (d2 == FunUtil.DoubleNull) {
            if (d1 == Double.NEGATIVE_INFINITY) {
                return -1;
            } else {
                return 1;
            }
        } else if (d1 < d2) {
            return -1;
        } else {
            return 1;
        }
    }

    public static int compareValues(int i, int j) {
        return (i == j)
            ? 0
            : (i < j)
                ? -1
                : 1;
    }

    /**
     * Compares two cell values.
     *
     * <p>Nulls compare last, exceptions (including the
     * object which indicates the the cell is not in the cache yet) next,
     * then numbers and strings are compared by value.
     *
     * @param value0 First cell value
     * @param value1 Second cell value
     * @return -1, 0, or 1, depending upon whether first cell value is less
     *   than, equal to, or greater than the second
     */
    public static int compareValues(Object value0, Object value1) {
        if (value0 == value1) {
            return 0;
        }
        // null is less than anything else
        if (value0 == null) {
            return -1;
        }
        if (value1 == null) {
            return 1;
        }
        if (value0 instanceof RuntimeException
            || value1 instanceof RuntimeException)
        {
            // one of the values is not in cache; continue as best as we can
            return 0;
        } else if (value0 == Util.nullValue) {
            return -1; // null == -infinity
        } else if (value1 == Util.nullValue) {
            return 1; // null == -infinity
        } else if (value0 instanceof String) {
            return ((String) value0).compareTo((String) value1);
        } else if (value0 instanceof Number) {
            return FunUtil.compareValues(
                    ((Number) value0).doubleValue(),
                    ((Number) value1).doubleValue());
        } else if (value0 instanceof OrderKey) {
            return ((OrderKey) value0).compareTo(value1);
        } else {
            throw Util.newInternal("cannot compare " + value0);
        }
    }

    /**
     * Turns the mapped values into relative values (percentages) for easy
     * use by the general topOrBottom function. This might also be a useful
     * function in itself.
     */
    static void toPercent(
        List members,
        Map mapMemberToValue,
        boolean isMember)
    {
        double total = 0;
        int memberCount = members.size();
        for (int i = 0; i < memberCount; i++) {
            Object o = (isMember)
                    ? mapMemberToValue.get(members.get(i))
                    : mapMemberToValue.get(
                        new ArrayHolder<Member>((Member []) members.get(i)));
            if (o instanceof Number) {
                total += ((Number) o).doubleValue();
            }
        }
        for (int i = 0; i < memberCount; i++) {
            Object mo = members.get(i);
            Object o =
                (isMember)
                ? mapMemberToValue.get(mo)
                : mapMemberToValue.get(new ArrayHolder<Member>((Member []) mo));
            if (o instanceof Number) {
                double d = ((Number) o).doubleValue();
                if (isMember) {
                    mapMemberToValue.put(
                        mo,
                        d / total * (double) 100);
                } else {
                    mapMemberToValue.put(
                        new ArrayHolder<Member>((Member []) mo),
                        d / total * (double) 100);
                }
            }
        }
    }


    /**
     * Decodes the syntactic type of an operator.
     *
     * @param flags A encoded string which represents an operator signature,
     *   as used by the {@code flags} parameter used to construct a
     *   {@link FunDefBase}.
     *
     * @return A {@link Syntax}
     */
    public static Syntax decodeSyntacticType(String flags) {
        char c = flags.charAt(0);
        switch (c) {
        case 'p':
            return Syntax.Property;
        case 'f':
            return Syntax.Function;
        case 'm':
            return Syntax.Method;
        case 'i':
            return Syntax.Infix;
        case 'P':
            return Syntax.Prefix;
        case 'Q':
            return Syntax.Postfix;
        case 'I':
            return Syntax.Internal;
        default:
            throw newInternal(
                "unknown syntax code '" + c + "' in string '" + flags + "'");
        }
    }

    /**
     * Decodes the signature of a function into a category code which describes
     * the return type of the operator.
     *
     * <p>For example, <code>decodeReturnType("fnx")</code> returns
     * <code>{@link Category#Numeric}</code>, indicating this function has a
     * numeric return value.
     *
     * @param flags The signature of an operator,
     *   as used by the {@code flags} parameter used to construct a
     *   {@link FunDefBase}.
     *
     * @return An array {@link Category} codes.
     */
    public static int decodeReturnCategory(String flags) {
        final int returnCategory = decodeCategory(flags, 1);
        if ((returnCategory & Category.Mask) != returnCategory) {
            throw newInternal("bad return code flag in flags '" + flags + "'");
        }
        return returnCategory;
    }

    /**
     * Decodes the {@code offset}th character of an encoded method
     * signature into a type category.
     *
     * <p>The codes are:
     * <table border="1">
     *
     * <tr><td>a</td><td>{@link Category#Array}</td></tr>
     *
     * <tr><td>d</td><td>{@link Category#Dimension}</td></tr>
     *
     * <tr><td>h</td><td>{@link Category#Hierarchy}</td></tr>
     *
     * <tr><td>l</td><td>{@link Category#Level}</td></tr>
     *
     * <tr><td>b</td><td>{@link Category#Logical}</td></tr>
     *
     * <tr><td>m</td><td>{@link Category#Member}</td></tr>
     *
     * <tr><td>N</td><td>Constant {@link Category#Numeric}</td></tr>
     *
     * <tr><td>n</td><td>{@link Category#Numeric}</td></tr>
     *
     * <tr><td>x</td><td>{@link Category#Set}</td></tr>
     *
     * <tr><td>#</td><td>Constant {@link Category#String}</td></tr>
     *
     * <tr><td>S</td><td>{@link Category#String}</td></tr>
     *
     * <tr><td>t</td><td>{@link Category#Tuple}</td></tr>
     *
     * <tr><td>v</td><td>{@link Category#Value}</td></tr>
     *
     * <tr><td>y</td><td>{@link Category#Symbol}</td></tr>
     *
     * </table>
     *
     * @param flags Encoded signature string
     * @param offset 0-based offset of character within string
     * @return A {@link Category}
     */
    public static int decodeCategory(String flags, int offset) {
        char c = flags.charAt(offset);
        switch (c) {
        case 'a':
            return Category.Array;
        case 'd':
            return Category.Dimension;
        case 'h':
            return Category.Hierarchy;
        case 'l':
            return Category.Level;
        case 'b':
            return Category.Logical;
        case 'm':
            return Category.Member;
        case 'N':
            return Category.Numeric | Category.Constant;
        case 'n':
            return Category.Numeric;
        case 'I':
            return Category.Numeric | Category.Integer | Category.Constant;
        case 'i':
            return Category.Numeric | Category.Integer;
        case 'x':
            return Category.Set;
        case '#':
            return Category.String | Category.Constant;
        case 'S':
            return Category.String;
        case 't':
            return Category.Tuple;
        case 'v':
            return Category.Value;
        case 'y':
            return Category.Symbol;
        case 'U':
            return Category.Null;
        case 'e':
            return Category.Empty;
        case 'D':
            return Category.DateTime;
        default:
            throw newInternal(
                    "unknown type code '" + c + "' in string '" + flags + "'");
        }
    }

    /**
     * Decodes a string of parameter types into an array of type codes.
     *
     * <p>Each character is decoded using {@link #decodeCategory(String, int)}.
     * For example, <code>decodeParameterTypes("nx")</code> returns
     * <code>{{@link Category#Numeric}, {@link Category#Set}}</code>.
     *
     * @param flags The signature of an operator,
     *   as used by the {@code flags} parameter used to construct a
     *   {@link FunDefBase}.
     *
     * @return An array {@link Category} codes.
     */
    public static int[] decodeParameterCategories(String flags) {
        int[] parameterCategories = new int[flags.length() - 2];
        for (int i = 0; i < parameterCategories.length; i++) {
            parameterCategories[i] = decodeCategory(flags, i + 2);
        }
        return parameterCategories;
    }

    /**
     * Converts a double (primitive) value to a Double. {@link #DoubleNull}
     * becomes null.
     */
    public static Double box(double d) {
        return d == DoubleNull
            ? null
            : d;
    }

    /**
     * Converts an int (primitive) value to an Integer. {@link #IntegerNull}
     * becomes null.
     */
    public static Integer box(int n) {
        return n == IntegerNull
            ? null
            : n;
    }

    /**
     * Sorts an array of values.
     */
    public static void sortValuesDesc(Object[] values) {
        Arrays.sort(values, DescendingValueComparator.instance);
    }

    /**
     * Binary searches an array of values.
     */
    public static int searchValuesDesc(Object[] values, Object value) {
        return Arrays.binarySearch(
                values, value, DescendingValueComparator.instance);
    }

    static double percentile(
        Evaluator evaluator,
        List members,
        Calc exp,
        double p)
    {
        SetWrapper sw = evaluateSet(evaluator, members, exp);
        if (sw.errorCount > 0) {
            return Double.NaN;
        } else if (sw.v.size() == 0) {
            return FunUtil.DoubleNull;
        }
        double[] asArray = new double[sw.v.size()];
        for (int i = 0; i < asArray.length; i++) {
            asArray[i] = (Double) sw.v.get(i);
        }
        Arrays.sort(asArray);

        /*
         * The median is defined as the value that has exactly the same
         * number of entries before it in the sorted list as after.
         * So, if the number of entries in the list is odd, the
         * median is the entry at (length-1)/2 (using zero-based indexes).
         * If the number of entries is even, the median is defined as the
         * arithmetic mean of the two numbers in the middle of the list, or
         * (entries[length/2 - 1] + entries[length/2]) / 2.
         */
        int length = asArray.length;

        if (p == 0.5) {
            // Special case for median.
            if ((length & 1) == 1) {
                // The length is odd. Note that length/2 is an integer
                // expression, and it's positive so we save ourselves a divide.
                return asArray[length >> 1];
            } else {
                return (asArray[(length >> 1) - 1] + asArray[length >> 1])
                    / 2.0;
            }
        } else if (p <= 0.0) {
            return asArray[0];
        } else if (p >= 1.0) {
            return asArray[length - 1];
        } else {
            final double jD = Math.floor(length * p);
            int j = (int) jD;
            double alpha = (p - jD) * length;
            assert alpha >= 0;
            assert alpha <= 1;
            return asArray[j] * (1.0 - alpha)
                + asArray[j + 1] * alpha;
        }
    }

    /**
     * Returns the member which lies upon a particular quartile according to a
     * given expression.
     *
     * @param evaluator Evaluator
     * @param members List of members
     * @param exp Expression to rank members
     * @param range Quartile (1, 2 or 3)
     *
     * @pre range >= 1 && range <= 3
     */
    protected static double quartile(
        Evaluator evaluator,
        List members,
        Calc exp,
        int range)
    {
        assert range >= 1 && range <= 3;

        SetWrapper sw = evaluateSet(evaluator, members, exp);
        if (sw.errorCount > 0) {
            return Double.NaN;
        } else if (sw.v.size() == 0) {
            return DoubleNull;
        }

        double[] asArray = new double[sw.v.size()];
        for (int i = 0; i < asArray.length; i++) {
            asArray[i] = ((Double) sw.v.get(i)).doubleValue();
        }

        Arrays.sort(asArray);
        // get a quartile, median is a second q
        double dm = 0.25 * asArray.length * range;
        int median = (int) Math.floor(dm);
        return dm == median && median < asArray.length - 1
            ? (asArray[median] + asArray[median + 1]) / 2
            : asArray[median];
    }

    public static Object min(Evaluator evaluator, List members, Calc calc) {
        SetWrapper sw = evaluateSet(evaluator, members, calc);
        if (sw.errorCount > 0) {
            return Double.NaN;
        } else {
            final int size = sw.v.size();
            if (size == 0) {
                return Util.nullValue;
            } else {
                Double min = (Double) sw.v.get(0);
                for (int i = 1; i < size; i++) {
                    Double iValue = (Double) sw.v.get(i);
                    if (iValue < min) {
                        min = iValue;
                    }
                }
                return min;
            }
        }
    }

    public static Object max(Evaluator evaluator, List members, Calc exp) {
        SetWrapper sw = evaluateSet(evaluator, members, exp);
        if (sw.errorCount > 0) {
            return Double.NaN;
        } else {
            final int size = sw.v.size();
            if (size == 0) {
                return Util.nullValue;
            } else {
                Double max = (Double) sw.v.get(0);
                for (int i = 1; i < size; i++) {
                    Double iValue = (Double) sw.v.get(i);
                    if (iValue > max) {
                        max = iValue;
                    }
                }
                return max;
            }
        }
    }

    static Object var(
        Evaluator evaluator,
        List members,
        Calc exp,
        boolean biased)
    {
        SetWrapper sw = evaluateSet(evaluator, members, exp);
        return _var(sw, biased);
    }

    private static Object _var(SetWrapper sw, boolean biased) {
        if (sw.errorCount > 0) {
            return new Double(Double.NaN);
        } else if (sw.v.size() == 0) {
            return Util.nullValue;
        } else {
            double stdev = 0.0;
            double avg = _avg(sw);
            for (int i = 0; i < sw.v.size(); i++) {
                stdev +=
                    Math.pow((((Double) sw.v.get(i)).doubleValue() - avg), 2);
            }
            int n = sw.v.size();
            if (!biased) {
                n--;
            }
            return new Double(stdev / (double) n);
        }
    }

    static double correlation(
        Evaluator evaluator,
        List memberList,
        Calc exp1,
        Calc exp2)
    {
        SetWrapper sw1 = evaluateSet(evaluator, memberList, exp1);
        SetWrapper sw2 = evaluateSet(evaluator, memberList, exp2);
        Object covar = _covariance(sw1, sw2, false);
        Object var1 = _var(sw1, false); //this should be false, yes?
        Object var2 = _var(sw2, false);
        if ((covar instanceof Double)
            && (var1 instanceof Double)
            && (var2 instanceof Double))
        {
            return ((Double) covar).doubleValue()
                / Math.sqrt(
                    ((Double) var1).doubleValue()
                    * ((Double) var2).doubleValue());
        } else {
            return DoubleNull;
        }
    }

    static Object covariance(
        Evaluator evaluator,
        List members,
        Calc exp1,
        Calc exp2,
        boolean biased)
    {
        SetWrapper sw1 = evaluateSet(evaluator.push(), members, exp1);
        SetWrapper sw2 = evaluateSet(evaluator.push(), members, exp2);
        // todo: because evaluateSet does not add nulls to the SetWrapper, this
        // solution may lead to mismatched lists and is therefore not robust
        return _covariance(sw1, sw2, biased);
    }


    private static Object _covariance(
        SetWrapper sw1,
        SetWrapper sw2,
        boolean biased)
    {
        if (sw1.v.size() != sw2.v.size()) {
            return Util.nullValue;
        }
        double avg1 = _avg(sw1);
        double avg2 = _avg(sw2);
        double covar = 0.0;
        for (int i = 0; i < sw1.v.size(); i++) {
            // all of this casting seems inefficient - can we make SetWrapper
            // contain an array of double instead?
            double diff1 = (((Double) sw1.v.get(i)).doubleValue() - avg1);
            double diff2 = (((Double) sw2.v.get(i)).doubleValue() - avg2);
            covar += (diff1 * diff2);
        }
        int n = sw1.v.size();
        if (!biased) {
            n--;
        }
        return new Double(covar / (double) n);
    }

    static Object stdev(
        Evaluator evaluator,
        List members,
        Calc exp,
        boolean biased)
    {
        Object o = var(evaluator, members, exp, biased);
        return (o instanceof Double)
            ? new Double(Math.sqrt(((Double) o).doubleValue()))
            : o;
    }

    public static Object avg(
        Evaluator evaluator,
        List members,
        Calc calc)
    {
        SetWrapper sw = evaluateSet(evaluator, members, calc);
        return (sw.errorCount > 0)
            ? new Double(Double.NaN)
            : (sw.v.size() == 0)
            ? Util.nullValue
            : new Double(_avg(sw));
    }

    // TODO: parameterize inclusion of nulls; also, maybe make _avg a method of
    // setwrapper, so we can cache the result (i.e. for correl)
    private static double _avg(SetWrapper sw) {
        double sum = 0.0;
        for (int i = 0; i < sw.v.size(); i++) {
            sum += ((Double) sw.v.get(i)).doubleValue();
        }
        //todo: should look at context and optionally include nulls
        return sum / (double) sw.v.size();
    }

    public static Object sum(Evaluator evaluator, List members, Calc exp) {
        double d = sumDouble(evaluator, members, exp);
        return d == DoubleNull ? Util.nullValue : new Double(d);
    }

    public static double sumDouble(
        Evaluator evaluator,
        List members,
        Calc exp)
    {
        SetWrapper sw = evaluateSet(evaluator, members, exp);
        if (sw.errorCount > 0) {
            return Double.NaN;
        } else if (sw.v.size() == 0) {
            return DoubleNull;
        } else {
            double sum = 0.0;
            for (int i = 0; i < sw.v.size(); i++) {
                sum += ((Double) sw.v.get(i)).doubleValue();
            }
            return sum;
        }
    }

    public static double sumDouble(
        Evaluator evaluator,
        Iterable iterable,
        Calc exp)
    {
        SetWrapper sw = evaluateSet(evaluator, iterable, exp);
        if (sw.errorCount > 0) {
            return Double.NaN;
        } else if (sw.v.size() == 0) {
            return DoubleNull;
        } else {
            double sum = 0.0;
            for (int i = 0; i < sw.v.size(); i++) {
                sum += ((Double) sw.v.get(i)).doubleValue();
            }
            return sum;
        }
    }

    public static int count(
        Evaluator evaluator,
        Iterable iterable,
        boolean includeEmpty)
    {
        if (iterable == null) {
            return 0;
        }
        if (includeEmpty) {
            if (iterable instanceof Collection) {
                return ((Collection) iterable).size();
            } else {
                int retval = 0;
                Iterator it = iterable.iterator();
                while (it.hasNext()) {
                    // must get the next one
                    it.next();
                    retval++;
                }
                return retval;
            }
        } else {
            int retval = 0;
            for (Object object : iterable) {
                if (object instanceof Member) {
                    evaluator.setContext((Member) object);
                } else {
                    evaluator.setContext((Member[]) object);
                }
                Object o = evaluator.evaluateCurrent();
                if (o != Util.nullValue && o != null) {
                    retval++;
                }
            }
            return retval;
        }
    }

    /**
     * Evaluates {@code exp} (if defined) over {@code members} to
     * generate a {@link List} of {@link SetWrapper} objects, which contains
     * a {@link Double} value and meta information, unlike
     * {@link #evaluateMembers}, which only produces values.
     *
     * @pre exp != null
     */
    static SetWrapper evaluateSet(
        Evaluator evaluator,
        Iterable members,
        Calc calc)
    {
        Util.assertPrecondition(members != null, "members != null");
        Util.assertPrecondition(calc != null, "calc != null");
        Util.assertPrecondition(calc.getType() instanceof ScalarType);

        // todo: treat constant exps as evaluateMembers() does
        SetWrapper retval = new SetWrapper();
        for (Object obj : members) {
            if (obj instanceof Member[]) {
                evaluator.setContext((Member[])obj);
            } else {
                evaluator.setContext((Member)obj);
            }
            Object o = calc.evaluate(evaluator);
            if (o == null || o == Util.nullValue) {
                retval.nullCount++;
            } else if (o instanceof Throwable) {
                // Carry on summing, so that if we are running in a
                // BatchingCellReader, we find out all the dependent cells we
                // need
                retval.errorCount++;
            } else if (o instanceof Double) {
                retval.v.add(o);
            } else if (o instanceof Number) {
                retval.v.add(((Number) o).doubleValue());
            } else {
                retval.v.add(o);
            }
        }
        return retval;
    }

    /**
     * Evaluates one or more expressions against the member list returning
     * a SetWrapper array. Where this differs very significantly from the
     * above evaluateSet methods is how it count null values and Throwables;
     * this method adds nulls to the SetWrapper Vector rather than not adding
     * anything - as the above method does. The impact of this is that if, for
     * example, one was creating a list of x,y values then each list will have
     * the same number of values (though some might be null) - this allows
     * higher level code to determine how to handle the lack of data rather than
     * having a non-equal number (if one is plotting x,y values it helps to
     * have the same number and know where a potential gap is the data is.
     */
    static SetWrapper[] evaluateSet(
        Evaluator evaluator,
        List members,
        DoubleCalc[] calcs,
        boolean isTuples)
    {
        Util.assertPrecondition(calcs != null, "calcs != null");

        // todo: treat constant exps as evaluateMembers() does
        SetWrapper[] retvals = new SetWrapper[calcs.length];
        for (int i = 0; i < calcs.length; i++) {
            retvals[i] = new SetWrapper();
        }
        for (final Object member : members) {
            if (isTuples) {
                evaluator.setContext((List<Member>) member);
            } else {
                evaluator.setContext((Member) member);
            }
            for (int i = 0; i < calcs.length; i++) {
                DoubleCalc calc = calcs[i];
                SetWrapper retval = retvals[i];
                double o = calc.evaluateDouble(evaluator);
                if (o == FunUtil.DoubleNull) {
                    retval.nullCount++;
                    retval.v.add(null);
                } else {
                    retval.v.add(o);
                }
                // TODO: If the expression yielded an error, carry on
                // summing, so that if we are running in a
                // BatchingCellReader, we find out all the dependent cells
                // we need
            }
        }
        return retvals;
    }

    static List<Member> periodsToDate(
        Evaluator evaluator,
        Level level,
        Member member)
    {
        if (member == null) {
            member = evaluator.getContext(level.getHierarchy());
        }
        Member m = member;
        while (m != null) {
            if (m.getLevel() == level) {
                break;
            }
            m = m.getParentMember();
        }
        // If m == null, then "level" was lower than member's level.
        // periodsToDate([Time].[Quarter], [Time].[1997] is valid,
        //  but will return an empty List
        List<Member> members = new ArrayList<Member>();
        if (m != null) {
            // e.g. m is [Time].[1997] and member is [Time].[1997].[Q1].[3]
            // we now have to make m to be the first member of the range,
            // so m becomes [Time].[1997].[Q1].[1]
            SchemaReader reader = evaluator.getSchemaReader();
            m = Util.getFirstDescendantOnLevel(reader, m, member.getLevel());
            reader.getMemberRange(level, m, member, members);
        }
        return members;
    }

    static List<Member> memberRange(
        Evaluator evaluator,
        Member startMember,
        Member endMember)
    {
        final Level level = startMember.getLevel();
        assertTrue(level == endMember.getLevel());
        List<Member> members = new ArrayList<Member>();
        evaluator.getSchemaReader().getMemberRange(
            level, startMember, endMember, members);

        if (members.isEmpty()) {
            // The result is empty, so maybe the members are reversed. This is
            // cheaper than comparing the members before we call getMemberRange.
            evaluator.getSchemaReader().getMemberRange(
                level, endMember, startMember, members);
        }
        return members;
    }

    /**
     * Returns the member under ancestorMember having the same relative position
     * under member's parent.
     * <p>For exmaple, cousin([Feb 2001], [Q3 2001]) is [August 2001].
     * @param schemaReader The reader to use
     * @param member The member for which we'll find the cousin.
     * @param ancestorMember The cousin's ancestor.
     *
     * @return The child of {@code ancestorMember} in the same position
     * under {@code ancestorMember} as {@code member} is under its
     * parent.
     */
    static Member cousin(
        SchemaReader schemaReader,
        Member member,
        Member ancestorMember)
    {
        if (ancestorMember.isNull()) {
            return ancestorMember;
        }
        if (member.getHierarchy() != ancestorMember.getHierarchy()) {
            throw MondrianResource.instance().CousinHierarchyMismatch.ex(
                member.getUniqueName(), ancestorMember.getUniqueName());
        }
        if (member.getLevel().getDepth()
            < ancestorMember.getLevel().getDepth())
        {
            return member.getHierarchy().getNullMember();
        }

        Member cousin = cousin2(schemaReader, member, ancestorMember);
        if (cousin == null) {
            cousin = member.getHierarchy().getNullMember();
        }

        return cousin;
    }

    static private Member cousin2(
        SchemaReader schemaReader,
        Member member1,
        Member member2)
    {
        if (member1.getLevel() == member2.getLevel()) {
            return member2;
        }
        Member uncle =
            cousin2(schemaReader, member1.getParentMember(), member2);
        if (uncle == null) {
            return null;
        }
        int ordinal = Util.getMemberOrdinalInParent(schemaReader, member1);
        List<Member> cousins = schemaReader.getMemberChildren(uncle);
        if (cousins.size() <= ordinal) {
            return null;
        }
        return cousins.get(ordinal);
    }

    /**
     * Returns the ancestor of {@code member} at the given level
     * or distance. It is assumed that any error checking required
     * has been done prior to calling this function.
     *
     * <p>This method takes into consideration the fact that there
     * may be intervening hidden members between {@code member}
     * and the ancestor. If {@code targetLevel} is not null, then
     * the method will only return a member if the level at
     * {@code distance} from the member is actually the
     * {@code targetLevel} specified.
     *
     * @param evaluator The evaluation context
     * @param member The member for which the ancestor is to be found
     * @param distance The distance up the chain the ancestor is to
     * be found.
     *
     * @param targetLevel The desired targetLevel of the ancestor. If
     * {@code null}, then the distance completely determines the desired
     * ancestor.
     *
     * @return The ancestor member, or {@code null} if no such
     * ancestor exists.
     */
    static Member ancestor(
        Evaluator evaluator,
        Member member,
        int distance,
        Level targetLevel)
    {
        if ((targetLevel != null)
            && (member.getHierarchy() != targetLevel.getHierarchy()))
        {
            throw MondrianResource.instance().MemberNotInLevelHierarchy.ex(
                member.getUniqueName(), targetLevel.getUniqueName());
        }

        if (distance == 0) {
            /*
            * Shortcut if there's nowhere to go.
            */
            return member;
        } else if (distance < 0) {
            /*
            * Can't go backwards.
            */
            return member.getHierarchy().getNullMember();
        }

        List<Member> ancestors = member.getAncestorMembers();
        final SchemaReader schemaReader = evaluator.getSchemaReader();

        Member result = member.getHierarchy().getNullMember();

        searchLoop:
        for (int i = 0; i < ancestors.size(); i++) {
            final Member ancestorMember = ancestors.get(i);

            if (targetLevel != null) {
                if (ancestorMember.getLevel() == targetLevel) {
                    if (schemaReader.isVisible(ancestorMember)) {
                        result = ancestorMember;
                        break;
                    } else {
                        result = member.getHierarchy().getNullMember();
                        break;
                    }
                }
            } else {
                if (schemaReader.isVisible(ancestorMember)) {
                    distance--;

                    // Make sure that this ancestor is really on the right
                    // targetLevel. If a targetLevel was specified and at least
                    // one of the ancestors was hidden, this this algorithm goes
                    // too far up the ancestor list. It's not a problem, except
                    // that we need to check if it's happened and return the
                    // hierarchy's null member instead.
                    //
                    // For example, consider what happens with
                    // Ancestor([Store].[Israel].[Haifa], [Store].[Store
                    // State]).  The distance from [Haifa] to [Store State] is
                    // 1, but that lands us at the country targetLevel, which is
                    // clearly wrong.
                    if (distance == 0) {
                        result = ancestorMember;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Compares a pair of members according to their positions in a
     * prefix-order (or postfix-order, if {@code post} is true) walk
     * over a hierarchy.
     *
     * @param m1 First member
     * @param m2 Second member
     *
     * @param post Whether to sortMembers in postfix order. If true, a parent
     *   will sortMembers immediately after its last child. If false, a parent
     *   will sortMembers immediately before its first child.
     *
     * @return -1 if m1 collates before m2,
     *   0 if m1 equals m2,
     *   1 if m1 collates after m2
     */
    public static int compareHierarchically(
        Member m1,
        Member m2,
        boolean post)
    {
        // Strip away the LimitedRollupMember wrapper, if it exists. The
        // wrapper does not implement equals and comparisons correctly. This
        // is safe this method has no side-effects: it just returns an int.
        if (m1 instanceof RolapHierarchy.LimitedRollupMember) {
            m1 = ((RolapHierarchy.LimitedRollupMember) m1).member;
        }
        if (m2 instanceof RolapHierarchy.LimitedRollupMember) {
            m2 = ((RolapHierarchy.LimitedRollupMember) m2).member;
        }
        if (equals(m1, m2)) {
            return 0;
        }
        while (true) {
            int depth1 = m1.getDepth();
            int depth2 = m2.getDepth();
            if (depth1 < depth2) {
                m2 = m2.getParentMember();
                if (equals(m1, m2)) {
                    return post ? 1 : -1;
                }
            } else if (depth1 > depth2) {
                m1 = m1.getParentMember();
                if (equals(m1, m2)) {
                    return post ? -1 : 1;
                }
            } else {
                Member prev1 = m1;
                Member prev2 = m2;
                m1 = m1.getParentMember();
                m2 = m2.getParentMember();
                if (equals(m1, m2)) {
                    final int c = compareSiblingMembers(prev1, prev2);
                    // compareHierarchically needs to impose a total order;
                    // cannot return 0 for non-equal members
                    assert c != 0
                        : "Members " + prev1 + ", " + prev2
                        + " are not equal, but compare returned 0.";
                    return c;
                }
            }
        }
    }

    /**
     * Compares two members which are known to have the same parent.
     *
     * First, compare by ordinal.
     * This is only valid now we know they're siblings, because
     * ordinals are only unique within a parent.
     * If the dimension does not use ordinals, both ordinals
     * will be -1.
     *
     * <p>If the ordinals do not differ, compare using regular member
     * comparison.
     *
     * @param m1 First member
     * @param m2 Second member
     * @return -1 if m1 collates less than m2,
     *   1 if m1 collates after m2,
     *   0 if m1 == m2.
     */
    public static int compareSiblingMembers(Member m1, Member m2) {
        // calculated members collate after non-calculated
        final boolean calculated1 = m1.isCalculatedInQuery();
        final boolean calculated2 = m2.isCalculatedInQuery();
        if (calculated1) {
            if (!calculated2) {
                return 1;
            }
        } else {
            if (calculated2) {
                return -1;
            }
        }
        final Comparable k1 = m1.getOrderKey();
        final Comparable k2 = m2.getOrderKey();
        if ((k1 != null) && (k2 != null)) {
            return k1.compareTo(k2);
        } else {
            final int ordinal1 = m1.getOrdinal();
            final int ordinal2 = m2.getOrdinal();
            return (ordinal1 == ordinal2)
                ? m1.compareTo(m2)
                : (ordinal1 < ordinal2)
                ? -1
                : 1;
        }
    }

    /**
     * Returns whether one of the members in a tuple is null.
     */
    static boolean tupleContainsNullMember(Member[] tuple) {
        for (Member member : tuple) {
            if (member.isNull()) {
                return true;
            }
        }
        return false;
    }

    public static Member[] makeNullTuple(final TupleType tupleType) {
        Member[] members = new Member[tupleType.elementTypes.length];
        for (int i = 0; i < tupleType.elementTypes.length; i++) {
            MemberType type = (MemberType) tupleType.elementTypes[i];
            members[i] = makeNullMember(type);
        }
        return members;
    }

    static Member makeNullMember(MemberType memberType) {
        Hierarchy hierarchy = memberType.getHierarchy();
        if (hierarchy == null) {
            return NullMember;
        }
        return hierarchy.getNullMember();
    }

    /**
     * Validates the arguments to a function and resolves the function.
     *
     * @param validator Validator used to validate function arguments and
     *           resolve the function
     * @param funDef Function definition, or null to deduce definition from
     *           name, syntax and argument types
     * @param args    Arguments to the function
     * @param newArgs Output parameter for the resolved arguments
     * @param name    Function name
     * @param syntax Syntax style used to invoke function
     * @return resolved function definition
     */
    public static FunDef resolveFunArgs(
        Validator validator,
        FunDef funDef,
        Exp[] args,
        Exp[] newArgs,
        String name,
        Syntax syntax)
    {
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = validator.validate(args[i], false);
        }
        if (funDef == null || validator.alwaysResolveFunDef()) {
            funDef = validator.getDef(newArgs, name, syntax);
        }
        checkNativeCompatible(validator, funDef, newArgs);
        return funDef;
    }

    /**
     * Functions that dynamically return one or more members of the measures
     * dimension prevent us from using native evaluation.
     *
     * @param validator Validator used to validate function arguments and
     *           resolve the function
     * @param funDef Function definition, or null to deduce definition from
     *           name, syntax and argument types
     * @param args    Arguments to the function
     */
    private static void checkNativeCompatible(
            Validator validator,
            FunDef funDef,
            Exp[] args)
    {
        // If the first argument to a function is either:
        // 1) the measures dimension or
        // 2) a measures member where the function returns another member or
        //    a set,
        // then these are functions that dynamically return one or more
        // members of the measures dimension.  In that case, we cannot use
        // native cross joins because the functions need to be executed to
        // determine the resultant measures.
        //
        // As a result, we disallow functions like AllMembers applied on the
        // Measures dimension as well as functions like the range operator,
        // siblings, and lag, when the argument is a measure member.
        // However, we do allow functions like isEmpty, rank, and topPercent.
        //
        // Also, the Set and Parentheses functions are ok since they're
        // essentially just containers.
        Query query = validator.getQuery();
        if (!(funDef instanceof SetFunDef)
            && !(funDef instanceof ParenthesesFunDef)
            && query != null
            && query.nativeCrossJoinVirtualCube())
        {
            int[] paramCategories = funDef.getParameterCategories();
            if (paramCategories.length > 0) {
                final int cat0 = paramCategories[0];
                final Exp arg0 = args[0];
                switch (cat0) {
                case Category.Dimension:
                case Category.Hierarchy:
                    if (arg0 instanceof DimensionExpr
                        && ((DimensionExpr) arg0).getDimension().isMeasures()
                        && !(funDef instanceof HierarchyCurrentMemberFunDef))
                    {
                        query.setVirtualCubeNonNativeCrossJoin();
                    }
                    break;
                case Category.Member:
                    if (arg0 instanceof MemberExpr
                        && ((MemberExpr) arg0).getMember().isMeasure()
                        && isMemberOrSet(funDef.getReturnCategory()))
                    {
                        query.setVirtualCubeNonNativeCrossJoin();
                    }
                    break;
                }
            }
        }
    }

    private static boolean isMemberOrSet(int category) {
        return category == Category.Member || category == Category.Set;
    }

    static void appendTuple(StringBuilder buf, Member[] members) {
        buf.append("(");
        for (int j = 0; j < members.length; j++) {
            if (j > 0) {
                buf.append(", ");
            }
            Member member = members[j];
            buf.append(member.getUniqueName());
        }
        buf.append(")");
    }

    /**
     * Returns whether two tuples are equal.
     *
     * <p>The members are allowed to be in different positions. For example,
     * <code>([Gender].[M], [Store].[USA]) IS ([Store].[USA],
     * [Gender].[M])</code> returns {@code true}.
     */
    static boolean equalTuple(Member[] members0, Member[] members1) {
        final int count = members0.length;
        if (count != members1.length) {
            return false;
        }
        outer:
        for (int i = 0; i < count; i++) {
            // First check the member at the corresponding ordinal. It is more
            // likely to be there.
            final Member member0 = members0[i];
            if (member0.equals(members1[i])) {
                continue;
            }
            // Look for this member in other positions.
            // We can assume that the members in members0 are distinct (because
            // they belong to different dimensions), so this test is valid.
            for (int j = 0; j < count; j++) {
                if (i != j && member0.equals(members1[j])) {
                    continue outer;
                }
            }
            // This member of members0 does not occur in any position of
            // members1. The tuples are not equal.
            return false;
        }
        return true;
    }

    static FunDef createDummyFunDef(
        Resolver resolver,
        int returnCategory,
        Exp[] args)
    {
        final int[] argCategories = ExpBase.getTypes(args);
        return new FunDefBase(resolver, returnCategory, argCategories) {
        };
    }

    public static List<Member> getNonEmptyMemberChildren(
        Evaluator evaluator,
        Member member)
    {
        SchemaReader sr = evaluator.getSchemaReader();
        if (evaluator.isNonEmpty()) {
            return sr.getMemberChildren(member, evaluator);
        } else {
            return sr.getMemberChildren(member);
        }
    }

    /**
     * Returns members of a level which are not empty (according to the
     * criteria expressed by the evaluator).
     *
     * @param evaluator Evaluator, determines non-empty criteria
     * @param level Level
     * @param includeCalcMembers Whether to include calculated members
     */
    static List<Member> getNonEmptyLevelMembers(
        final Evaluator evaluator,
        final Level level,
        final boolean includeCalcMembers)
    {
        SchemaReader sr = evaluator.getSchemaReader();
        if (evaluator.isNonEmpty()) {
            List<Member> members = sr.getLevelMembers(level, evaluator);
            if (includeCalcMembers) {
                return addLevelCalculatedMembers(sr, level, members);
            }
            return members;
        }
        return sr.getLevelMembers(level, includeCalcMembers);
    }

    static List<Member> levelMembers(
        final Level level,
        final Evaluator evaluator,
        final boolean includeCalcMembers)
    {
        List<Member> memberList =
            getNonEmptyLevelMembers(evaluator, level, includeCalcMembers);
        if (!includeCalcMembers) {
            memberList = removeCalculatedMembers(memberList);
        }
        hierarchizeMemberList(memberList, false);
        return memberList;
    }

    static List<Member> hierarchyMembers(
        Hierarchy hierarchy,
        Evaluator evaluator,
        final boolean includeCalcMembers)
    {
        List<Member> memberList;
        if (evaluator.isNonEmpty()) {
            // Allow the SQL generator to generate optimized SQL since we know
            // we're only interested in non-empty members of this level.
            memberList = new ArrayList<Member>();
            for (Level level : hierarchy.getLevels()) {
                List<Member> members =
                    getNonEmptyLevelMembers(
                        evaluator, level, includeCalcMembers);
                memberList.addAll(members);
            }
        } else {
            memberList = addMembers(
                evaluator.getSchemaReader(),
                new ConcatenableList<Member>(), hierarchy);
            if (!includeCalcMembers && memberList != null) {
                memberList = removeCalculatedMembers(memberList);
            }
        }
        hierarchizeMemberList(memberList, false);
        return memberList;
    }

    static List<Member> dimensionMembers(
        Dimension dimension,
        Evaluator evaluator,
        final boolean includeCalcMembers)
    {
        Hierarchy hierarchy = dimension.getHierarchy();
        return hierarchyMembers(hierarchy, evaluator, includeCalcMembers);
    }

   /**
    * Partial Sort: sorts in place an array of Objects using a given Comparator,
    * but only enough so that the N biggest (or smallest) items are at the start
    * of the array. Not a stable sort, unless the Comparator is so contrived.
    *
    * @param items will be partially-sorted in place
    * @param comp a Comparator; null means use natural comparison
    * @param limit
    */
    static void partialSort(Object[] items, Comparator comp, int limit)
    {
        if (comp == null) {
            comp = ComparatorUtils.naturalComparator();
        }
        new Quicksorter(items, comp).partialSort(limit);
    }

    /**
     * Stable partial sort of a list. Returns the desired head of the list.
     */
    static <T> List<T> stablePartialSort(
        final List<T> list, final Comparator<T> comp, int limit)
    {
        assert limit >= 0;

        // Load an array of pairs {list-item, list-index}.
        // List-index is a secondary sort key, to give a stable sort.
        // REVIEW Can we use a simple T[], with the index implied?
        // REVIEW When limit is big relative to list size, faster to
        // mergesort. Test for this.
        int n = list.size();            // O(n) to scan list
        Pair<T, Integer>[] pairs = new Pair[n];

        int i = 0;
        for (T item : list) {           // O(n) to scan list
            pairs[i] = new Pair(item, i);
            ++i;
        }

        Comparator<Pair<T, Integer>> pairComp =
            new Comparator<Pair<T, Integer>>()
        {
            public int compare(Pair<T, Integer> x, Pair<T, Integer> y) {
                int val = comp.compare(x.left, y.left);
                if (val == 0) {
                    val = x.right.compareTo(y.right);
                }
                return val;
            }
        };

        limit = Math.min(limit, n);
        // O(n + limit * log(limit)) to quicksort
        partialSort(pairs, pairComp, limit);

        List<T> result = new ArrayList<T>();
        for (i = 0; i < limit; ++i) {
            result.add((T) pairs[i].left);  // O(limit) to scan
        }
        return result;
    }

    // ~ Inner classes ---------------------------------------------------------

    /**
     * A functional for {@link FunUtil#partialSort}.
     * Sorts or partially sorts an array in ascending order, using a Comparator.
     *
     * <p>Algorithm: quicksort, or partial quicksort (alias
     * "quickselect"), Hoare/Singleton.  Partial quicksort is
     * quicksort that recurs only on one side, which is thus
     * tail-recursion.  Picks pivot as median of three; falls back on
     * insertion sort for small "subfiles".  Partial quicksort is O(n
     * + m log m), instead of O(n log n), where n is the input size,
     * and m is the desired output size.
     *
     * <p>See D Knuth, Art of Computer Programming, 5.2.2 (Algorithm
     * Q); R. Sedgewick, Algorithms in C, ch 5.  Good summary in
     * http://en.wikipedia.org/wiki/Selection_algorithm
     *
     * TODO: What is the time-cost of this functor and of the nested
     * Comparators?
     */
    static class Quicksorter<T> {
        // size of smallest set worth a quicksort
        public final int TOO_SMALL = 8;

        private static final Logger LOGGER =
            Logger.getLogger(Quicksorter.class);
        private final T[] vec;
        private final Comparator<T> comp;
        private final boolean traced;
        private long partitions, comparisons, exchanges; // stats

        public Quicksorter(T[] vec, Comparator<T> comp) {
            this.vec = vec;
            this.comp = comp;
            partitions = comparisons = exchanges = 0;
            traced = LOGGER.isDebugEnabled();
        }

        private void traceStats(String prefix) {
            StringBuilder sb = new StringBuilder(prefix);
            sb.append(": ");
            sb.append(partitions).append(" partitions, ");
            sb.append(comparisons).append(" comparisons, ");
            sb.append(exchanges).append(" exchanges.");
            LOGGER.debug(sb.toString());
        }

        // equivalent to operator <
        private boolean less(T x, T y) {
            comparisons++;
            return comp.compare(x, y) < 0;
        }

        // equivalent to operator >
        private boolean more(T x, T y) {
            comparisons++;
            return comp.compare(x, y) > 0;
        }
        // equivalent to operator >
        private boolean equal(T x, T y) {
            comparisons++;
            return comp.compare(x, y) == 0;
        }

        // swaps two items (identified by index in vec[])
        private void swap(int i, int j) {
            exchanges++;
            T temp = vec[i];
            vec[i] = vec[j];
            vec[j] = temp;
        }

        // puts into ascending order three items
        // (identified by index in vec[])
        // REVIEW: use only 2 comparisons??
        private void order3(int i, int j, int k) {
            if (more(vec[i], vec[j])) {
                swap(i, j);
            }
            if (more(vec[i], vec[k])) {
                swap(i, k);
            }
            if (more(vec[j], vec[k])) {
                swap(j, k);
            }
        }

        // runs a selection sort on the array segment VEC[START .. END]
        private void selectionSort(int start, int end) {
            for (int i = start; i < end; ++i) {
                // pick the min of vec[i, end]
                int pmin = i;
                for (int j = i + 1; j <= end; ++j) {
                    if (less(vec[j], vec[pmin])) {
                        pmin = j;
                    }
                }
                if (pmin != i) {
                    swap(i, pmin);
                }
            }
        }

        /**
         * Runs one pass of quicksort on array segment VEC[START .. END],
         * dividing it into two parts, the left side VEC[START .. P] none
         * greater than the pivot value VEC[P], and the right side VEC[P+1
         * .. END] none less than the pivot value. Returns P, the index of the
         * pivot element in VEC[].
         */
        private int partition(int start, int end) {
            partitions++;
            assert start <= end;

            // Find median of three (both ends and the middle).
            // TODO: use pseudo-median of nine when array segment is big enough.
            int mid = (start + end) / 2;
            order3(start, mid, end);
            if (end - start <= 2) {
                return mid;        // sorted!
            }

            // Now the left and right ends are in place (ie in the correct
            // partition), and will serve as sentinels for scanning. Pick middle
            // as pivot and set it aside, in penultimate position.
            final T pivot = vec[mid];
            swap(mid, end - 1);

            // Scan inward from both ends, swapping misplaced items.
            int left = start + 1;       // vec[start] is in place
            int right = end - 2;        // vec[end - 1] is pivot
            while (left < right) {
                // scan past items in correct place, but stop at a pivot value
                // (Sedgewick's idea).
                while (less(vec[left], pivot)) {
                    ++left;
                }
                while (less(pivot, vec[right])) {
                    --right;
                }
                if (debug) {
                    assert (left <= end) && (right >= start);
                }
                if (left < right) {     // found a misplaced pair
                    swap(left, right);
                    ++left; --right;
                }
            }
            if ((left == right) && less(vec[left], pivot)) {
                ++left;
            }

            // All scanned. Restore pivot to its rightful place.
            swap(left, end - 1);

            if (debug) {
                for (int i = start; i < left; i++) {
                    assert !more(vec[i], pivot);
                }
                assert equal(vec[left], pivot);
                for (int i = left + 1;  i <= end;  i++) {
                    assert !less(vec[i], pivot);
                }
            }
            return left;
        }


        // Runs quicksort on VEC[START, END]. Recursive version,
        // TODO: exploit tail recursion
        private void sort(int start, int end) {
            if (end - start < TOO_SMALL) {
                selectionSort(start, end);
                return;
            }

            // Split data, so that left side dominates the right side
            // (but neither is sorted):
            int mid = partition(start, end);
            sort(start, mid - 1);
            sort(mid + 1, end);
        }

        // Runs quickselect(LIMIT) on VEC[START, END]. Recursive version,
        // TODO: exploit tail recursion, unfold.
        private void select(int limit, int start, int end) {
            if (limit == 0) {
                return;
            }
            if (end - start < TOO_SMALL) {
                selectionSort(start, end);
                return;
            }
            int mid = partition(start, end);
            int leftSize = mid - start + 1;
            if (limit < leftSize) {
                // work on the left side, and ignore the right side
                select(limit, start, mid);
            } else {
                limit -= leftSize;
                // work on the right side, but keep the left side
                select(limit, mid + 1, end);
            }
        }

        public void sort() {
            int n = vec.length - 1;
            sort(0, n);
            if (traced) {
                traceStats("quicksort on " + n + "items");
            }
        }

        /** puts the LIMIT biggest items at the head, not sorted */
        public void select(int limit) {
            int n = vec.length - 1;
            select(limit, 0, n);
            if (traced) {
                traceStats("quickselect for " + limit + " from" + n + "items");
            }
        }

        public void partialSort(int limit) {
            int n = vec.length - 1;
            select(limit, 0, n);
            if (traced) {
                traceStats(
                    "partial sort: quickselect phase for "
                    + limit + "from " + n + "items");
            }
            sort(0, limit - 1);
            if (traced) {
                traceStats("partial sort: quicksort phase on " + n + "items");
            }
        }
    }

    /**
     * Comparator for members.
     *
     * <p>Could genericize this to <code>class&lt;T&gt; MemorizingComparator
     * implements Comparator&lt;T&gt;</code>, but not if it adds a run time
     * cost, since the comparator is at the heart of the sort algorithms.
     */
    private static abstract class MemberComparator implements Comparator<Member>
    {
        private static final Logger LOGGER =
            Logger.getLogger(MemberComparator.class);
        final Evaluator evaluator;
        final Calc exp;
        final private boolean desc;
        final private Map<Member, Object> valueMap;

        MemberComparator(Evaluator evaluator, Calc exp, boolean desc)
        {
            this.evaluator = evaluator;
            this.exp = exp;
            this.desc = desc;
            this.valueMap = new HashMap<Member, Object>();
        }

        // applies the Calc to a member, memorizing results
        protected Object eval(Member m)
        {
            Object val = valueMap.get(m);
            if (val == null) {
                evaluator.setContext(m);
                val = exp.evaluate(evaluator);
                if (val == null) {
                    val = Util.nullValue;
                }
                valueMap.put(m, val);
            }
            return val;
        }

        // wraps comparison with tracing
        Comparator<Member> wrap() {
            final MemberComparator comparator = this;
            if (LOGGER.isDebugEnabled()) {
                return new Comparator<Member>() {
                    public int compare(Member m1, Member m2) {
                        final int c = comparator.compare(m1, m2);
                        // here guaranteed that eval(m) finds a memorized value
                        LOGGER.debug(
                            "compare "
                            + m1.getUniqueName() + "(" + eval(m1) + "), "
                            + m2.getUniqueName() + "(" + eval(m2) + ")"
                            + " yields " + c);
                        return c;
                    }
                };
            } else {
                return this;
            }
        }

        // Preloads the value map with precomputed members (supplied as a map).
        void preloadValues(Map<Member, Object> map) {
            valueMap.putAll(map);
        }

        // Preloads the value map by applying the expression to a Collection of
        // members.
        void preloadValues(Collection<Member> members) {
            for (Member m : members) {
                eval(m);
            }
        }

        protected final int compareByValue(Member m1, Member m2) {
            final int c = FunUtil.compareValues(eval(m1), eval(m2));
            return desc ? -c : c;
        }

        protected final int compareHierarchicallyButSiblingsByValue(
            Member m1, Member m2)
        {
            if (FunUtil.equals(m1, m2)) {
                return 0;
            }
            while (true) {
                int depth1 = m1.getDepth(),
                    depth2 = m2.getDepth();
                if (depth1 < depth2) {
                    m2 = m2.getParentMember();
                    if (Util.equals(m1, m2)) {
                        return -1;
                    }
                } else if (depth1 > depth2) {
                    m1 = m1.getParentMember();
                    if (Util.equals(m1, m2)) {
                        return 1;
                    }
                } else {
                    Member prev1 = m1, prev2 = m2;
                    m1 = m1.getParentMember();
                    m2 = m2.getParentMember();
                    if (Util.equals(m1, m2)) {
                        // including case where both parents are null
                        int c = compareByValue(prev1, prev2);
                        if (c != 0) {
                            return c;
                        }
                        // prev1 and prev2 are siblings.  Order according to
                        // hierarchy, if the values do not differ.  Needed to
                        // have a consistent sortMembers if members with equal
                        // (null!)  values are compared.
                        c = FunUtil.compareSiblingMembers(prev1, prev2);
                        return c;
                    }
                }
            }
        }
    }

    private static class HierarchicalMemberComparator
        extends MemberComparator
    {
        HierarchicalMemberComparator(
            Evaluator evaluator, Calc exp, boolean desc)
        {
            super(evaluator, exp, desc);
        }

        public int compare(Member m1, Member m2) {
            return compareHierarchicallyButSiblingsByValue(m1, m2);
        }
    }

    private static class BreakMemberComparator extends MemberComparator {
        BreakMemberComparator(Evaluator evaluator, Calc exp, boolean desc)
        {
            super(evaluator, exp, desc);
        }

        public final int compare(Member m1, Member m2) {
            return compareByValue(m1, m2);
        }
    }

    /**
     * Compares tuples, which are represented as arrays of {@link Member}s.
     */
    private static abstract class ArrayComparator
        implements Comparator<Member[]>
    {
        private static final Logger LOGGER =
            Logger.getLogger(ArrayComparator.class);
        final int arity;

        ArrayComparator(int arity) {
            this.arity = arity;
        }

        Comparator<Member[]> wrap() {
            final ArrayComparator base = this;
            if (LOGGER.isDebugEnabled()) {
                return new Comparator<Member[]>() {
                    public int compare(Member[] a1, Member[] a2) {
                        int c = base.compare(a1, a2);
                        LOGGER.debug(
                            "compare {" + toString(a1) + "}, {"
                            + toString(a2) + "} yields " + c);
                        return c;
                    }

                    private String toString(Member[] a) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < a.length; i++) {
                            Member member = a[i];
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append(member.getUniqueName());
                        }
                        return sb.toString();
                    }
                };
            } else {
                return this;
            }
        }
    }

    /**
     * Extension to {@link ArrayComparator} which compares tuples by evaluating
     * an expression.
     */
    private static abstract class ArrayExpComparator extends ArrayComparator {
        Evaluator evaluator;
        final Calc calc;

        ArrayExpComparator(Evaluator evaluator, Calc calc, int arity) {
            super(arity);
            this.evaluator = evaluator;
            this.calc = calc;
        }
    }

    private static class HierarchicalArrayComparator
        extends ArrayExpComparator
    {
        private final boolean desc;

        HierarchicalArrayComparator(
            Evaluator evaluator, Calc calc, int arity, boolean desc)
        {
            super(evaluator, calc, arity);
            this.desc = desc;
        }

        public int compare(Member[] a1, Member[] a2) {
            int c = 0;
            evaluator = evaluator.push();
            for (int i = 0; i < arity; i++) {
                Member m1 = a1[i], m2 = a2[i];
                c = compareHierarchicallyButSiblingsByValue(m1, m2);
                if (c != 0) {
                    break;
                }
                // compareHierarchicallyButSiblingsByValue imposes a total order
                Util.assertTrue(m1.equals(m2));
                evaluator.setContext(m1);
            }
            evaluator = evaluator.pop();
            return c;
        }

        protected int compareHierarchicallyButSiblingsByValue(
            Member m1,
            Member m2)
        {
            if (FunUtil.equals(m1, m2)) {
                return 0;
            }
            while (true) {
                int depth1 = m1.getDepth(),
                        depth2 = m2.getDepth();
                if (depth1 < depth2) {
                    m2 = m2.getParentMember();
                    if (FunUtil.equals(m1, m2)) {
                        return -1;
                    }
                } else if (depth1 > depth2) {
                    m1 = m1.getParentMember();
                    if (FunUtil.equals(m1, m2)) {
                        return 1;
                    }
                } else {
                    Member prev1 = m1, prev2 = m2;
                    m1 = m1.getParentMember();
                    m2 = m2.getParentMember();
                    if (FunUtil.equals(m1, m2)) {
                        // including case where both parents are null
                        int c = compareByValue(prev1, prev2);
                        if (c == 0) {
                            c = FunUtil.compareSiblingMembers(prev1, prev2);
                        }
                        return desc ? -c : c;
                    }
                }
            }
        }

        private int compareByValue(Member m1, Member m2) {
            int c;
            Member old = evaluator.setContext(m1);
            Object v1 = calc.evaluate(evaluator);
            evaluator.setContext(m2);
            Object v2 = calc.evaluate(evaluator);
            // important to restore the evaluator state -- and this is faster
            // than calling evaluator.push()
            evaluator.setContext(old);
            c = FunUtil.compareValues(v1, v2);
            return c;
        }
    }

    // almost the same as MemberComparator
    static abstract class ArrayExpMemoComparator extends ArrayExpComparator {
        final private Map<Object, Object> valueMap;

        ArrayExpMemoComparator(Evaluator e, Calc calc, int arity)
        {
            super(e, calc, arity);
            this.valueMap = new HashMap<Object, Object>();
        }

        // applies the Calc to a tuple, memorizing results
        protected Object eval(Member[] t)
        {
            Object val = valueMap.get(t);
            if (val == null) {
                evaluator.setContext(t);
                val = calc.evaluate(evaluator);
                if (val == null) {
                    val = Util.nullValue;
                }
                valueMap.put(t, val);
            }
            return val;
        }

        // Preloads the value map with precomputed members (supplied as a map).
        void preloadValues(Map<Member[], Object> map) {
            valueMap.putAll(map);
        }

        // Preloads the value map by applying the expression to a Collection of
        // members.
        void preloadValues(Collection<Member[]> tuples) {
            for (Member[] t : tuples) {
                eval(t);
            }
        }
    }

    private static class BreakArrayComparator extends ArrayExpMemoComparator {
        BreakArrayComparator(Evaluator e, Calc calc, int arity) {
            super(e, calc, arity);
        }

        public int compare(Member[] a1, Member[] a2) {
            return FunUtil.compareValues(eval(a1), eval(a2));
        }
    }

    private static class HierarchicalArrayKeyComparator
        extends ArrayExpMemoComparator
    {

        HierarchicalArrayKeyComparator(Evaluator e, Calc calc, int arity) {
            super(e, calc, arity);
        }

        public int compare(Member[] a1, Member[] a2) {
            OrderKey k1 = (OrderKey) eval(a1);
            OrderKey k2 = (OrderKey) eval(a2);
            return compareMemberOrderKeysHierarchically(k1, k2);
        }

        private int compareMemberOrderKeysHierarchically(
            OrderKey k1, OrderKey k2)
        {
            // null is less than anything else
            if (k1 == Util.nullValue) {
                return -1;
            }
            if (k2 == Util.nullValue) {
                return 1;
            }
            Member m1 = k1.member;
            Member m2 = k2.member;
            if (FunUtil.equals(m1, m2)) {
                return 0;
            }
            while (true) {
                int depth1 = m1.getDepth(),
                        depth2 = m2.getDepth();
                if (depth1 < depth2) {
                    m2 = m2.getParentMember();
                    if (FunUtil.equals(m1, m2)) {
                        return -1;
                    }
                } else if (depth1 > depth2) {
                    m1 = m1.getParentMember();
                    if (FunUtil.equals(m1, m2)) {
                        return 1;
                    }
                } else {
                    Member prev1 = m1, prev2 = m2;
                    m1 = m1.getParentMember();
                    m2 = m2.getParentMember();
                    if (FunUtil.equals(m1, m2)) {
                        OrderKey pk1 = new OrderKey(prev1);
                        OrderKey pk2 = new OrderKey(prev2);
                        return FunUtil.compareValues(pk1, pk2);
                    }
                }
            }
        }
    }

    /**
     * Compares arrays of {@link Member}s so as to convert them into hierarchical
     * order. Applies lexicographic order to the array.
     */
    private static class HierarchizeArrayComparator extends ArrayComparator {
        private final boolean post;

        HierarchizeArrayComparator(int arity, boolean post) {
            super(arity);
            this.post = post;
        }

        public int compare(Member[] a1, Member[] a2) {
            for (int i = 0; i < arity; i++) {
                Member m1 = a1[i], m2 = a2[i];
                int c = FunUtil.compareHierarchically(m1, m2, post);
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        }
    }

    /**
     * Compares {@link Member}s so as to arrage them in prefix or postfix
     * hierarchical order.
     */
    private static class HierarchizeComparator implements Comparator<Member> {
        private final boolean post;

        HierarchizeComparator(boolean post) {
            this.post = post;
        }
        public int compare(Member m1, Member m2) {
            return FunUtil.compareHierarchically(m1, m2, post);
        }
    }

    /**
     * Reverses the order of a {@link Comparator}.
     */
    private static class ReverseComparator<T> implements Comparator<T> {
        Comparator<T> comparator;
        ReverseComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        public int compare(T o1, T o2) {
            int c = comparator.compare(o1, o2);
            return - c;
        }
    }

    static class SetWrapper {
        List v = new ArrayList();
        public int errorCount = 0, nullCount = 0;

        //private double avg = Double.NaN;
        //todo: parameterize inclusion of nulls
        //by making this a method of the SetWrapper, we can cache the result
        //this allows its reuse in Correlation
        // public double getAverage() {
        //     if (avg == Double.NaN) {
        //         double sum = 0.0;
        //         for (int i = 0; i < resolvers.size(); i++) {
        //             sum += ((Double) resolvers.elementAt(i)).doubleValue();
        //         }
        //         //todo: should look at context and optionally include nulls
        //         avg = sum / (double) resolvers.size();
        //     }
        //     return avg;
        // }
    }

    /**
     * Compares cell values, so that larger values compare first.
     *
     * <p>Nulls compare last, exceptions (including the
     * object which indicates the the cell is not in the cache yet) next,
     * then numbers and strings are compared by value.
     */
    public static class DescendingValueComparator implements Comparator {
        /**
         * The singleton.
         */
        static final DescendingValueComparator instance =
                new DescendingValueComparator();

        public int compare(Object o1, Object o2) {
            return - compareValues(o1, o2);
        }
    }

    /**
     * Null member of unknown hierarchy.
     */
    private static class NullMember implements Member {
        public Member getParentMember() {
            throw new UnsupportedOperationException();
        }

        public Level getLevel() {
            throw new UnsupportedOperationException();
        }

        public Hierarchy getHierarchy() {
            throw new UnsupportedOperationException();
        }

        public String getParentUniqueName() {
            throw new UnsupportedOperationException();
        }

        public MemberType getMemberType() {
            throw new UnsupportedOperationException();
        }

        public boolean isParentChildLeaf() {
            return false;
        }

        public void setName(String name) {
            throw new UnsupportedOperationException();
        }

        public boolean isAll() {
            return false;
        }

        public boolean isMeasure() {
            throw new UnsupportedOperationException();
        }

        public boolean isNull() {
            return true;
        }

        public boolean isChildOrEqualTo(Member member) {
            throw new UnsupportedOperationException();
        }

        public boolean isCalculated() {
            throw new UnsupportedOperationException();
        }

        public boolean isEvaluated() {
            throw new UnsupportedOperationException();
        }

        public int getSolveOrder() {
            throw new UnsupportedOperationException();
        }

        public Exp getExpression() {
            throw new UnsupportedOperationException();
        }

        public List<Member> getAncestorMembers() {
            throw new UnsupportedOperationException();
        }

        public boolean isCalculatedInQuery() {
            throw new UnsupportedOperationException();
        }

        public Object getPropertyValue(String propertyName) {
            throw new UnsupportedOperationException();
        }

        public Object getPropertyValue(String propertyName, boolean matchCase) {
            throw new UnsupportedOperationException();
        }

        public String getPropertyFormattedValue(String propertyName) {
            throw new UnsupportedOperationException();
        }

        public void setProperty(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        public Property[] getProperties() {
            throw new UnsupportedOperationException();
        }

        public int getOrdinal() {
            throw new UnsupportedOperationException();
        }

        public Comparable getOrderKey() {
            throw new UnsupportedOperationException();
        }

        public boolean isHidden() {
            throw new UnsupportedOperationException();
        }

        public int getDepth() {
            throw new UnsupportedOperationException();
        }

        public Member getDataMember() {
            throw new UnsupportedOperationException();
        }

        public String getUniqueName() {
            throw new UnsupportedOperationException();
        }

        public String getName() {
            throw new UnsupportedOperationException();
        }

        public String getDescription() {
            throw new UnsupportedOperationException();
        }

        public OlapElement lookupChild(
            SchemaReader schemaReader, Id.Segment s, MatchType matchType)
        {
            throw new UnsupportedOperationException();
        }

        public String getQualifiedName() {
            throw new UnsupportedOperationException();
        }

        public String getCaption() {
            throw new UnsupportedOperationException();
        }

        public Dimension getDimension() {
            throw new UnsupportedOperationException();
        }

        public Map<String, Annotation> getAnnotationMap() {
            throw new UnsupportedOperationException();
        }

        public int compareTo(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean equals(Object obj) {
            throw new UnsupportedOperationException();
        }

        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Enumeration of the flags allowed to the {@code ORDER} MDX function.
     */
    enum Flag {
        ASC(false, false),
        DESC(true, false),
        BASC(false, true),
        BDESC(true, true);

        final boolean descending;
        final boolean brk;

        Flag(boolean descending, boolean brk) {
            this.descending = descending;
            this.brk = brk;
        }

        public static String[] getNames() {
            List<String> names = new ArrayList<String>();
            for (Flag flags : Flag.class.getEnumConstants()) {
                names.add(flags.name());
            }
            return names.toArray(new String[names.size()]);
        }
    }

    static class SortKeySpec {
        private final Calc key;
        private final Flag direction;

        SortKeySpec(Calc key, Flag dir) {
            this.key = key;
            this.direction = dir;
        }

        Calc getKey() {
            return this.key;
        }

        Flag getDirection() {
            return this.direction;
        }
    }

    public static class OrderKey implements Comparable {
        private final Member member;

        public OrderKey(Member member) {
            super();
            this.member = member;
        }

        public int compareTo(Object o) {
            assert o instanceof OrderKey;
            Member otherMember = ((OrderKey) o).member;
            final boolean thisCalculated = this.member.isCalculatedInQuery();
            final boolean otherCalculated = otherMember.isCalculatedInQuery();
            if (thisCalculated) {
                if (!otherCalculated) {
                    return 1;
                }
            } else {
                if (otherCalculated) {
                    return -1;
                }
            }
            final Comparable thisKey = this.member.getOrderKey();
            final Comparable otherKey = otherMember.getOrderKey();
            if ((thisKey != null) && (otherKey != null)) {
                return thisKey.compareTo(otherKey);
            } else {
                return this.member.compareTo(otherMember);
            }
        }
    }
}

// End FunUtil.java

