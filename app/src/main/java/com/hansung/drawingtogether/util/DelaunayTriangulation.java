package com.hansung.drawingtogether.util;

/*
 * error
 */

class Panic {
	static public void panic(String m) {
		System.err.println(m);
		System.exit(1);
	}
}

/*
 * Warpper class Int
 */
class Int {
	private int i;

	public Int() {
		i = 0;
	}

	public Int(int i) {
		this.i = i;
	}

	public void setValue(int i) {
		this.i = i;
	}

	public int getValue() {
		return i;
	}
}


class Edge {
	int s, t;
	int l, r;

	Edge() { s = t = 0; }
	Edge(int s, int t) { this.s =s; this.t = t; }
	int s() { return this.s; }
	int t() { return this.t; }
	int l() { return this.l; }
	int r() { return this.r; }
}

class Vector {
	float u, v;

	Vector() { u = v = 0.0f; }
	Vector(RealPoint p1, RealPoint p2) { 
		u = p2.x - p1.x; 
		v = p2.y - p1.y; 
	}
	Vector(float u, float v) { this.u = u; this.v = v; }

	float dotProduct(Vector v) { return u * v.u + this.v * v.v; }

	static float dotProduct(RealPoint p1, RealPoint p2, RealPoint p3) { 
		float u1, v1, u2, v2;

		u1 =  p2.x - p1.x;
		v1 =  p2.y - p1.y;
		u2 =  p3.x - p1.x;
		v2 =  p3.y - p1.y;

		return u1 * u2 + v1 * v2;
	}

	float crossProduct(Vector v) { return u * v.v - this.v * v.u; }

	static float crossProduct(RealPoint p1, RealPoint p2, RealPoint p3) { 
		float u1, v1, u2, v2;

		u1 =  p2.x - p1.x;
		v1 =  p2.y - p1.y;
		u2 =  p3.x - p1.x;
		v2 =  p3.y - p1.y;

		return u1 * v2 - v1 * u2;
	}

	void setRealPoints(RealPoint p1, RealPoint p2) { 
		u = p2.x - p1.x; 
		v = p2.y - p1.y; 
	}
}

/*
 * Circle class. 
 */
class Circle {
	RealPoint c;
	float r;

	Circle() { c = new RealPoint(); r = 0.0f; }
	Circle(RealPoint c, float r) { this.c = c; this.r = r; }
	public RealPoint center() { return c; }
	public float radius() { return r; }
	public void set(RealPoint c, float r) { this.c = c; this.r = r; }

	/* 
	 * Tests if a point lies inside the circle instance.
	 */
	public boolean inside(RealPoint p) {
		if (c.distanceSq(p) < r * r)
			return true;
		else
			return false;
	}

	/* 
	 * Compute the circle defined by three points (circumcircle). 
	 */
	public void circumCircle(RealPoint p1, RealPoint p2, RealPoint p3) {
		float cp;

		cp = Vector.crossProduct(p1, p2, p3);
		if (cp != 0.0)
		{
			float p1Sq, p2Sq, p3Sq;
			float num;
			float cx, cy;

			p1Sq = p1.x * p1.x + p1.y * p1.y;
			p2Sq = p2.x * p2.x + p2.y * p2.y;
			p3Sq = p3.x * p3.x + p3.y * p3.y;
			num = p1Sq*(p2.y - p3.y) + p2Sq*(p3.y - p1.y) + p3Sq*(p1.y - p2.y);
			cx = num / (2.0f * cp);
			num = p1Sq*(p3.x - p2.x) + p2Sq*(p1.x - p3.x) + p3Sq*(p2.x - p1.x);
			cy = num / (2.0f * cp);

			c.x = cx;
			c.y = cy;
		}

		// Radius 
		r = c.distance(p1);
	}
}


class QuadraticAlgorithm {

	int triangleCount = 0;

	////////////
	int s, t, u, bP;
	Circle bC = new Circle();
	int nFaces;

	public QuadraticAlgorithm() {}

	public void triangulate(DelaunayTriangulation tri) {
		int currentEdge;
		int nFaces;
		Int s, t;

		// Initialise. 
		nFaces = 0;
		s = new Int();
		t = new Int();

		// Find closest neighbours and add edge to triangulation. 
		findClosestNeighbours(tri.point, tri.nPoints, s, t);

		// Create seed edge and add it to the triangulation. 
		tri.addEdge(s.getValue(), t.getValue(), 
				DelaunayTriangulation.Undefined,
				DelaunayTriangulation.Undefined);

		currentEdge = 0;
		while (currentEdge < tri.nEdges) {
			if (tri.edge[currentEdge].l == DelaunayTriangulation.Undefined) {
				completeFacet(currentEdge, tri, nFaces);
			}
			if (tri.edge[currentEdge].r == DelaunayTriangulation.Undefined) {
				completeFacet(currentEdge, tri, nFaces);
			}
			currentEdge++;
		}
	}

	// Find the two closest points.  
	public void findClosestNeighbours(DoublePoint p[], int nPoints,
                                      Int u, Int v) {
		int i, j;
		float d, min;
		int s, t;

		s = t = 0;
		min = Float.MAX_VALUE;
		for (i = 0; i < nPoints-1; i++)
			for (j = i+1; j < nPoints; j++)
			{
				d = p[i].p1.distanceSq(p[j].p1);
				if (d < min)
				{
					s = i;
					t = j;
					min = d;
				}
			}
		u.setValue(s);
		v.setValue(t);
	}

	/* 
	 * Complete a facet by looking for the circle free point to the left
	 * of the edge "e_i".  Add the facet to the triangulation.
	 *
	 * This function is a bit long and may be better split.
	 */
	public void completeFacet(int eI, DelaunayTriangulation tri, int nFaces) {
		float cP;
		Edge e[] = tri.edge;
		DoublePoint p[] = tri.point;

		// Cache s and t. 
		if (e[eI].l == DelaunayTriangulation.Undefined)
		{
			s = e[eI].s;
			t = e[eI].t;
		}
		else if (e[eI].r == DelaunayTriangulation.Undefined)
		{
			s = e[eI].t;
			t = e[eI].s;
		}
		else
			// Edge already completed. 
			return;


		// Find a point on left of edge. 
		for (u = 0; u < tri.nPoints; u++)
		{
			if (u == s || u == t)
				continue;
			if (Vector.crossProduct(p[s].p1, p[t].p1, p[u].p1) > 0.0)
				break;
		}

		// Find best point on left of edge. 
		bP = u;
		if (bP < tri.nPoints)
		{
			bC.circumCircle(p[s].p1, p[t].p1, p[bP].p1);

			for (u = bP+1; u < tri.nPoints; u++)
			{
				if (u == s || u == t)
					continue;
				cP = Vector.crossProduct(p[s].p1, p[t].p1, p[u].p1);

				if (cP > 0.0)
					if (bC.inside(p[u].p1))
					{
						bP = u;
						bC.circumCircle(p[s].p1, p[t].p1, p[u].p1);
					}
			}
		}

		// Add new triangle or update edge info if s-t is on hull. 
		if (bP < tri.nPoints)
		{
			// Update face information of edge being completed. 
			tri.updateLeftFace(eI, s, t, nFaces);
			nFaces++;

			// Add new edge or update face info of old edge. 
			eI = tri.findEdge(bP, s);
			if (eI == DelaunayTriangulation.Undefined)
				// New edge. 
				eI = tri.addEdge(bP, s, nFaces, DelaunayTriangulation.Undefined);
			else
				// Old edge. 
				tri.updateLeftFace(eI, bP, s, nFaces);

			// Add new edge or update face info of old edge. 
			eI = tri.findEdge(t, bP);
			if (eI == DelaunayTriangulation.Undefined)
				// New edge.  
				eI = tri.addEdge(t, bP, nFaces, DelaunayTriangulation.Undefined);
			else
				// Old edge.  
				tri.updateLeftFace(eI, t, bP, nFaces); 
		} else{
			tri.updateLeftFace(eI, s, t, DelaunayTriangulation.Universe);
			return;
		}
		
		
		tri.srcTriangle.ptr[triangleCount*6]=(int)tri.point[bP].p1.x;
		tri.srcTriangle.ptr[triangleCount*6+1]=(int)tri.point[bP].p1.y;
		tri.srcTriangle.ptr[triangleCount*6+2]=(int)tri.point[s].p1.x;
		tri.srcTriangle.ptr[triangleCount*6+3]=(int)tri.point[s].p1.y;
		tri.srcTriangle.ptr[triangleCount*6+4]=(int)tri.point[t].p1.x;
		tri.srcTriangle.ptr[triangleCount*6+5]=(int)tri.point[t].p1.y;
		tri.dstTriangle.ptr[triangleCount*6]=(int)tri.point[bP].p2.x;
		tri.dstTriangle.ptr[triangleCount*6+1]=(int)tri.point[bP].p2.y;
		tri.dstTriangle.ptr[triangleCount*6+2]=(int)tri.point[s].p2.x;
		tri.dstTriangle.ptr[triangleCount*6+3]=(int)tri.point[s].p2.y;
		tri.dstTriangle.ptr[triangleCount*6+4]=(int)tri.point[t].p2.x;
		tri.dstTriangle.ptr[triangleCount*6+5]=(int)tri.point[t].p2.y;
		triangleCount++;
		
	}

}


public class DelaunayTriangulation {
	public static final int Undefined = -1;
	public static final int Universe = 0;
	public int nPoints;
	public DoublePoint point[];
	public int nEdges;
	private int maxEdges;
	public Edge edge[];
	public Triangle srcTriangle,dstTriangle;
	public int triangleCount;
	private QuadraticAlgorithm qa = new QuadraticAlgorithm();
	

	@SuppressWarnings("unchecked")
	public DelaunayTriangulation(java.util.Vector vector, Triangle srcT, Triangle dstT) {
		this.srcTriangle = srcT;
		this.dstTriangle = dstT;
		
		// Allocate points. 
		this.nPoints = vector.size();
		triangleCount = (nPoints-3)*2;
		srcTriangle.ptr = new int[triangleCount*6];
		dstTriangle.ptr = new int[triangleCount*6]; 
		this.point = new DoublePoint[nPoints];
		Object p;
		for (int i = 0; i < nPoints; i++){
			p = vector.get(i);
			if(p instanceof PointView.PointButton)
				point[i] = ((PointView.PointButton)p).dp;
			else
				point[i] = (DoublePoint) p;
		}

		// Allocate edges.
		maxEdges = 5 + (nPoints-4)*3;	// Max number of edges.
		edge = new Edge[maxEdges];
		for (int i = 0; i < maxEdges; i++)
			edge[i] = new Edge();
		nEdges = 0;		
	}
	
	public void Triangulation(){
		qa.triangulate(this);
	}
	

	public int addEdge(int s, int t) {
		return addEdge(s, t, Undefined, Undefined);
	}

	/* 
	 * Adds an edge to the triangulation. Store edges with lowest
	 * vertex first (easier to debug and makes no other ddd	`ifference).
	 */
	public int addEdge(int s, int t, int l, int r) {
		int e;

		// Add edge if not already in the triangulation. 
		e = findEdge(s, t);
		if (e == Undefined) 
			if (s < t)
			{
				edge[nEdges].s = s;
				edge[nEdges].t = t;
				edge[nEdges].l = l;
				edge[nEdges].r = r;
				return nEdges++;
			} 
			else
			{
				edge[nEdges].s = t;
				edge[nEdges].t = s;
				edge[nEdges].l = r;
				edge[nEdges].r = l;
				return nEdges++;
			}
		else
			return Undefined;
	}

	public int findEdge(int s, int t) {
		boolean edgeExists = false;
		int i;

		for (i = 0; i < nEdges; i++)
			if (edge[i].s == s && edge[i].t == t || 
					edge[i].s == t && edge[i].t == s) {
				edgeExists = true;
				break;
			}

		if (edgeExists)
			return i;
		else
			return Undefined;
	}

	/* 
	 * Update the left face of an edge. 
	 */
	public void updateLeftFace(int eI, int s, int t, int f) {
		if (!((edge[eI].s == s && edge[eI].t == t) ||
				(edge[eI].s == t && edge[eI].t == s)))
			Panic.panic("updateLeftFace: adj. matrix and edge table mismatch");
		if (edge[eI].s == s && edge[eI].l == DelaunayTriangulation.Undefined)
			edge[eI].l = f;
		else if (edge[eI].t == s && edge[eI].r == DelaunayTriangulation.Undefined)
			edge[eI].r = f;
		else
			Panic.panic("updateLeftFace: attempt to overwrite edge info");
	}
}
