package bigtrace.scene;

import static com.jogamp.opengl.GL.GL_FLOAT;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import bigtrace.BigTraceData;
import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.geometry.Plane3D;
import bigtrace.geometry.ShapeInterpolation;
import bigtrace.volume.VolumeMisc;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import tpietzsch.backend.jogl.JoglGpuContext;
import tpietzsch.shadergen.DefaultShader;
import tpietzsch.shadergen.Shader;
import tpietzsch.shadergen.generate.Segment;
import tpietzsch.shadergen.generate.SegmentTemplate;

public class VisPolygonFlat {
	
	public static final int CENTER_LINE=0, WIRE=1, SURFACE=2;
	private final Shader prog;

	private int vao;
	
	
	private float vertices[]; 
	private int nPointsN=0;
	private int nGridEdges=0;

	public int renderType = WIRE;
	private Vector4f l_color;	
	public float fLineThickness;


	private boolean initialized;
	


	public VisPolygonFlat()
	{
		final Segment pointVp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.vp" ).instantiate();
		final Segment pointFp = new SegmentTemplate( VisPolyLineScaled.class, "/scene/simple_color.fp" ).instantiate();
	
		
		prog = new DefaultShader( pointVp.getCode(), pointFp.getCode() );
	}
	
	
	public VisPolygonFlat(final ArrayList< RealPoint > points, final float fLineThickness_, final Color color_in,  final int nRenderType)
	{
		this();
		
		fLineThickness= fLineThickness_;	
		l_color = new Vector4f(color_in.getComponents(null));		
		renderType = nRenderType;
		setVertices(points);
		
	}
	
	public void setThickness(float fLineThickness_)
	{
		fLineThickness= fLineThickness_;
	}
	
	public void setColor(Color color_in)
	{
		l_color = new Vector4f(color_in.getComponents(null));
	}



	public void setParams(final ArrayList< RealPoint > points, final float fLineThickness_, final int nSectorN_, final Color color_in)
	{
		
		fLineThickness= fLineThickness_;		
		l_color = new Vector4f(color_in.getComponents(null));		
		setVertices(points);
	}
	
	public void setRenderType(int nRenderType_)
	{
		renderType = nRenderType_;
		
	}
	public int getRenderType()
	{
		return renderType;
		
	}
	public void setVertices( ArrayList< RealPoint > points)
	{
		if(renderType == VisPolyLineScaled.CENTER_LINE)
		{
			setVerticesCenterLine(points);
		}

		if(renderType == VisPolyLineScaled.WIRE)
		{
			setVerticesWire(points);
		}
		if(renderType == VisPolyLineScaled.SURFACE)
		{
			setVerticesCenterLine(points);
		}
	}
	
	public void setVerticesCenterLine( final ArrayList< RealPoint > points)
	{
		
		int i,j;
		
		nPointsN=points.size();
		vertices = new float [nPointsN*3];//assume 3D	

		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{
				vertices[i*3+j]=points.get(i).getFloatPosition(j);
			}			
		}
		initialized=false;
	}
	

	
	/** generates a wireframe mesh of a pipe around provided points **/
	public void setVerticesWire( final ArrayList< RealPoint > points)
	{
		
		int i,j;

		double nGridStep = 20;
		
		nPointsN=points.size();
		
		if (nPointsN<3)
			return;
		
		//get a plane of polygon
		Plane3D plane = new Plane3D(points.get(1),points.get(0),points.get(2));
		//get edges
		ArrayList<ArrayList< RealPoint >> edges = getPolygonEdgesPairPoints(points);
		//first edge line
		double [] lineP1 = edges.get(0).get(0).positionAsDoubleArray();
		double [] lineP2 = edges.get(0).get(1).positionAsDoubleArray();

		Line3D gridDirection = new Line3D(lineP1,lineP2);
		double [] gridPlaneN = new double [3];
		LinAlgHelpers.cross(plane.n, gridDirection.linev[1], gridPlaneN);
		
		//plane containing the first edge and perpendicular to the polygon plane 
		Plane3D gridPlane = new Plane3D();
		gridPlane.setVectors(lineP1,gridPlaneN);
		
		double maxDist = 0.0;
		double pointDist =0.0;
		for(i=2;i<points.size();i++)
		{
			pointDist=gridPlane.signedDistance(points.get(i));
			if(Math.abs(pointDist)>Math.abs(maxDist))
			{
				maxDist=pointDist;
			}
			
		}
		ArrayList<ArrayList< RealPoint >> gridLines = new ArrayList<ArrayList< RealPoint >>(); 
		double [] interSect = new double [3];
		
		double nGridStepEquidist = maxDist/Math.round(maxDist/nGridStep);
		for(double dShift=nGridStepEquidist;dShift<=Math.abs(maxDist);dShift+=nGridStepEquidist )
		{
			LinAlgHelpers.scale(gridPlane.n, dShift, lineP2);
			LinAlgHelpers.add(lineP2, lineP1, lineP2);
			gridPlane.setVectors(lineP2, gridPlane.n);
			ArrayList< RealPoint > gridEdge = new ArrayList< RealPoint >(); 
			for(j=1;j<edges.size();j++)
			{
				if( Intersections3D.planeEdgeIntersect(gridPlane, edges.get(j).get(0), edges.get(j).get(1), interSect))
				{
					gridEdge.add(new RealPoint(interSect));
				}
				if(gridEdge.size()==2)
				{
					gridLines.add(gridEdge);
					
				}
			}
		}
		
		
		//plane perpendicular to the first edge and perpendicular to the polygon plane 
		gridPlane.setVectors(lineP1,gridDirection.linev[1]);
		
		double minDist = Double.MAX_VALUE;
		maxDist = (-1)*Double.MAX_VALUE;
		pointDist =0.0;
		for(i=0;i<points.size();i++)
		{
			pointDist=gridPlane.signedDistance(points.get(i));
			if(pointDist>maxDist)
			{
				maxDist=pointDist;
			}
			if(pointDist<minDist)
			{
				minDist=pointDist;
			}
			
		}

		nGridStepEquidist = (maxDist-minDist)/Math.round((maxDist-minDist)/nGridStep);
		for(double dShift=minDist;dShift<=maxDist;dShift+=nGridStepEquidist )
		{
			LinAlgHelpers.scale(gridPlane.n, dShift, lineP2);
			LinAlgHelpers.add(lineP2, lineP1, lineP2);
			gridPlane.setVectors(lineP2, gridPlane.n);
			ArrayList< RealPoint > gridEdge = new ArrayList< RealPoint >(); 
			for(j=0;j<edges.size();j++)
			{
				if( Intersections3D.planeEdgeIntersect(gridPlane, edges.get(j).get(0), edges.get(j).get(1), interSect))
				{
					gridEdge.add(new RealPoint(interSect));
				}
				if(gridEdge.size()==2)
				{
					gridLines.add(gridEdge);
					
				}
			}
		}

		
		
		nGridEdges = gridLines.size();

		vertices = new float [nPointsN*3 + nGridEdges*6];//assume 3D	
		//vertices = new float [nGridEdges*6];//assume 3D	

		//outline
		
		for (i=0;i<nPointsN; i++)
		{
			for (j=0;j<3; j++)
			{
				vertices[i*3+j]=points.get(i).getFloatPosition(j);
			}			
		}
		//grid lines
		
		for (i=0;i<nGridEdges; i++)
		{
			for (j=0;j<3; j++)
			{
				vertices[nPointsN*3+i*6+j]=gridLines.get(i).get(0).getFloatPosition(j);
				vertices[nPointsN*3+i*6+3+j]=gridLines.get(i).get(1).getFloatPosition(j);
				//vertices[i*6+j]=gridLines.get(i).get(0).getFloatPosition(j);
				//vertices[i*6+3+j]=gridLines.get(i).get(1).getFloatPosition(j);

			}	
		}
		initialized=false;
	}
	

	
	/** OpenGL buffer binding, etc thing **/
	private void init( GL3 gl )
	{
		initialized = true;
		if(nPointsN>1)
		{

			// ..:: VERTEX BUFFER ::..
	
			final int[] tmp = new int[ 2 ];
			gl.glGenBuffers( 1, tmp, 0 );
			final int vbo = tmp[ 0 ];
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vbo );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, FloatBuffer.wrap( vertices ), GL.GL_STATIC_DRAW );
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );
	
	
			// ..:: VERTEX ARRAY OBJECT ::..
	
			gl.glGenVertexArrays( 1, tmp, 0 );
			vao = tmp[ 0 ];
			gl.glBindVertexArray( vao );
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, vbo );
			gl.glVertexAttribPointer( 0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0 );
			gl.glEnableVertexAttribArray( 0 );
			gl.glBindVertexArray( 0 );
		}
	}

	public void draw( GL3 gl, Matrix4fc pvm )
	{
		int nGridIt;
		if ( !initialized )
			init( gl );

		if(nPointsN>1)
		{

			JoglGpuContext context = JoglGpuContext.get( gl );
	
			prog.getUniformMatrix4f( "pvm" ).set( pvm );
			prog.getUniform4f("colorin").set(l_color);
			prog.setUniforms( context );
			prog.use( context );

	
			gl.glBindVertexArray( vao );
			

			if(renderType == VisPolyLineScaled.CENTER_LINE)
			{
				gl.glLineWidth(fLineThickness);
				gl.glDrawArrays( GL.GL_LINE_LOOP, 0, nPointsN);
			}
			
			if(renderType == VisPolyLineScaled.WIRE)
			{
				gl.glLineWidth(fLineThickness);
				gl.glDrawArrays( GL.GL_LINE_LOOP, 0, nPointsN);
				
				for(nGridIt = 0;nGridIt<nGridEdges;nGridIt++)
				{
					gl.glDrawArrays( GL.GL_LINE_STRIP, nPointsN+nGridIt*2, 2);
				}
			}

			if(renderType == VisPolyLineScaled.SURFACE)
			{
				gl.glLineWidth(1.0f);
				gl.glDrawArrays( GL.GL_TRIANGLE_FAN, 0, nPointsN);
				
			}
			gl.glBindVertexArray( 0 );
		}
	}
	
	public static ArrayList<ArrayList< RealPoint >> getPolygonEdgesPairPoints(final ArrayList< RealPoint > points)
	{
		ArrayList<ArrayList< RealPoint >> out = new ArrayList<ArrayList< RealPoint >>();
		ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
		for(int i =1;i<points.size();i++)
		{
			point_coords = new ArrayList< RealPoint >();
			point_coords.add(new RealPoint(points.get(i-1)));
			point_coords.add(new RealPoint(points.get(i)));
			out.add(point_coords);
		}
		point_coords = new ArrayList< RealPoint >();
		point_coords.add(points.get(points.size()-1));
		point_coords.add(points.get(0));
		out.add(point_coords);
		return out;
	}
	
}
