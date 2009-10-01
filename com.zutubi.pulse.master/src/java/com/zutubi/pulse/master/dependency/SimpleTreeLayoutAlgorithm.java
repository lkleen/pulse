package com.zutubi.pulse.master.dependency;

import com.zutubi.util.*;
import static com.zutubi.util.CollectionUtils.asPair;

import java.util.List;


/**
 * Implements a basic algorithm for laying out a tree in two-dimensional space
 * so that it can be visualised.  The layout begins with the root on the left
 * and leaves end up on the right.  Nodes are spread vertically to avoid
 * overlap in a simple and conservative fashion (i.e. more vertical space may
 * be used than is strictly required).  A resulting tree may look something
 * like:
 *
 * <pre>{@code
 *  b
 * a
 *    d
 *  c
 *    e
 * }</pre>
 *
 * if rendered in simple text.
 */
public class SimpleTreeLayoutAlgorithm<T>
{
    /**
     * Lays out the given tree in two dimensional space by assigning a point to
     * each node.  The returned tree contains nodes with the original node data
     * plus the nodes position as a point.
     *
     * @param root the root of the tree to lay out
     * @return a copy of the given tree with a point assigned to each node
     */
    public TreeNode<Pair<T, Point>> layout(TreeNode<T> root)
    {
        return layout(root, 0, new LayoutState());
    }

    /**
     * Given a laid-out tree, returns a point representing the bottom-right
     * corner of the area that the tree occupies.  That is, a point with the
     * largest x and largest y coordinates assigned to any nodes in the tree.
     *
     * @param root root of a layed-out tree to get the bounds for
     * @return a point representing the outer bounds of the tree
     */
    public Point getBounds(TreeNode<Pair<T, Point>> root)
    {
        final int maxX[] = new int[]{0};
        final int maxY[] = new int[]{0};

        root.depthFirstWalk(new UnaryProcedure<TreeNode<Pair<T, Point>>>()
        {
            public void process(TreeNode<Pair<T, Point>> node)
            {
                Point nodePosition = node.getData().second;
                if (nodePosition.getX() > maxX[0])
                {
                    maxX[0] = nodePosition.getX();
                }

                if (nodePosition.getY() > maxY[0])
                {
                    maxY[0] = nodePosition.getY();
                }
            }
        });

        return new Point(maxX[0], maxY[0]);
    }

    private TreeNode<Pair<T, Point>> layout(TreeNode<T> node, final int depth, final LayoutState state)
    {
        if (node.isLeaf())
        {
            return new TreeNode<Pair<T, Point>>(asPair(node.getData(), new Point(depth, state.allocateNextY())));
        }
        else
        {
            List<TreeNode<Pair<T,Point>>> children = CollectionUtils.map(node, new Mapping<TreeNode<T>, TreeNode<Pair<T, Point>>>()
            {
                public TreeNode<Pair<T, Point>> map(TreeNode<T> child)
                {
                    return layout(child, depth + 1, state);
                }
            });

            TreeNode<Pair<T, Point>> result = new TreeNode<Pair<T, Point>>(asPair(node.getData(), new Point(depth, getAverageY(children))));
            result.addAll(children);
            return result;
        }
    }

    private int getAverageY(List<TreeNode<Pair<T, Point>>> nodes)
    {
        int total = 0;
        for (TreeNode<Pair<T, Point>> node: nodes)
        {
            total += node.getData().second.getY();
        }

        return total / nodes.size();
    }

    private static class LayoutState
    {
        private int nextY = 0;

        public int allocateNextY()
        {
            int result = nextY;
            nextY += 2;
            return result;
        }
    }
}