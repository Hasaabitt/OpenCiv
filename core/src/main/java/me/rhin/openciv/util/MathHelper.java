package me.rhin.openciv.util;

import com.badlogic.gdx.math.Vector2;

import me.rhin.openciv.game.map.GameMap;

public class MathHelper {

	// FIXME: Move minDistance to shared class.

	public static int minDistance(int dist[], boolean sptSet[]) {
		// Initialize min value
		int min = GameMap.WIDTH * GameMap.HEIGHT, min_index = 0;

		for (int v = 0; v < GameMap.WIDTH * GameMap.HEIGHT; v++)
			if (sptSet[v] == false && dist[v] <= min) {
				min = dist[v];
				min_index = v;
			}

		return min_index;
	}

	public static boolean isInsidePolygon(Vector2[] vectors, Vector2 mouseVector, Vector2 mouseExtremeVector) {
		// Count intersections of the above line
		// with sides of polygon
		int count = 0, i = 0;
		do {
			int next = (i + 1) % 6;

			// Check if the line segment from 'p' to
			// 'extreme' intersects with the line
			// segment from 'polygon[i]' to 'polygon[next]'
			if (doIntersect(vectors[i], vectors[next], mouseVector, mouseExtremeVector)) {
				// If the point 'p' is colinear with line
				// segment 'i-next', then check if it lies
				// on segment. If it lies, return true, otherwise false
				if (orientation(vectors[i], mouseVector, vectors[next]) == 0) {
					return onSegment(vectors[i], mouseVector, vectors[next]);
				}

				count++;
			}
			i = next;
		} while (i != 0);

		return (count % 2 == 1);
	}

	private static boolean doIntersect(Vector2 p1, Vector2 q1, Vector2 p2, Vector2 q2) {
		int o1 = orientation(p1, q1, p2);
		int o2 = orientation(p1, q1, q2);
		int o3 = orientation(p2, q2, p1);
		int o4 = orientation(p2, q2, q1);

		// General case
		if (o1 != o2 && o3 != o4) {
			return true;
		}

		// Special Cases
		// p1, q1 and p2 are colinear and
		// p2 lies on segment p1q1
		if (o1 == 0 && onSegment(p1, p2, q1)) {
			return true;
		}

		// p1, q1 and p2 are colinear and
		// q2 lies on segment p1q1
		if (o2 == 0 && onSegment(p1, q2, q1)) {
			return true;
		}

		// p2, q2 and p1 are colinear and
		// p1 lies on segment p2q2
		if (o3 == 0 && onSegment(p2, p1, q2)) {
			return true;
		}

		// p2, q2 and q1 are colinear and
		// q1 lies on segment p2q2
		if (o4 == 0 && onSegment(p2, q1, q2)) {
			return true;
		}

		// Doesn't fall in any of the above cases
		return false;
	}

	private static boolean onSegment(Vector2 p, Vector2 q, Vector2 r) {
		return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y)
				&& q.y >= Math.min(p.y, r.y);
	}

	// To find orientation of ordered triplet (p, q, r).
	// The function returns following values
	// 0 --> p, q and r are colinear
	// 1 --> Clockwise
	// 2 --> Counterclockwise
	private static int orientation(Vector2 p, Vector2 q, Vector2 r) {
		int val = (int) ((q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y));

		if (val == 0) {
			return 0; // colinear
		}
		return (val > 0) ? 1 : 2; // clock or counterclock wise
	}

}
