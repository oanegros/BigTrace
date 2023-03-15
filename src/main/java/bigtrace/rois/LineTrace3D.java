package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.BigTraceData;
import bigtrace.geometry.LinInterp3D;
import bigtrace.geometry.ShapeInterpolation;
import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineScaled;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.WritablePolyline;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class LineTrace3D extends AbstractRoi3D implements Roi3D, WritablePolyline
{
	
	public ArrayList<RealPoint> vertices;
	public ArrayList<ArrayList<RealPoint>> segments;
	public VisPointsScaled verticesVis;
	public VisPolyLineScaled segmentsVis;
	
	/** linear intepolator **/
	private LinInterp3D linInter = null;
	/** last interpolation used. -1 means interpolators were not initialized or not up-to-date **/
	private int lastInterpolation = -1;


	public LineTrace3D(final Roi3DGroup preset_in)
	{
		type = Roi3D.LINE_TRACE;
		pointSize = preset_in.pointSize;
		lineThickness=preset_in.lineThickness;
		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());
		lineColor = new Color(preset_in.lineColor.getRed(),preset_in.lineColor.getGreen(),preset_in.lineColor.getBlue(),preset_in.lineColor.getAlpha());

		renderType= preset_in.renderType;
		
		vertices = new ArrayList<RealPoint>();
		segments = new ArrayList<ArrayList<RealPoint>>();
		verticesVis = new VisPointsScaled();
		verticesVis.setColor(pointColor);
		verticesVis.setSize(pointSize);
		
		segmentsVis = new VisPolyLineScaled();
		name = "trace"+Integer.toString(this.hashCode());

	}
	/** adds initial vertex **/
	public void addFirstPoint(final RealPoint in_)
	{
		vertices.add(new RealPoint(in_));
		verticesVis.setVertices(vertices);
	}
	
	public void addPointAndSegment(final RealPoint in_, final ArrayList<RealPoint> segments_)
	{
		//check if the new point is at the same place that previous or not
		double [] dist = new double [3];
		LinAlgHelpers.subtract(vertices.get(vertices.size()-1).positionAsDoubleArray(), in_.positionAsDoubleArray(), dist);
		if(LinAlgHelpers.length(dist)>0.000001)
		{
			vertices.add(new RealPoint(in_));
			verticesVis.setVertices(vertices);
			segments.add(segments_);
			segmentsVis = new VisPolyLineScaled(makeJointSegment( BigTraceData.shapeInterpolation),lineThickness, lineColor, renderType);
		}
	}
	
	/** removes last segment of the tracing.
	 * if there was just one spot, returns false**/
	public boolean removeLastSegment() 
	{
		
		vertices.remove(vertices.size()-1);
		verticesVis.setVertices(vertices);
		if(vertices.size()>0)
		{
			segments.remove(segments.size()-1);
			segmentsVis = new VisPolyLineScaled(makeJointSegment( BigTraceData.shapeInterpolation),lineThickness, lineColor, renderType);
			return true;
		}
		else
		{
			return false;
		}
		
	}
	
	/** returns the last segment of the tracing.**/
	public ArrayList<RealPoint> getLastSegment() 
	{
		
		return segments.get(segments.size()-1);
		
	}


	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) {
		

		verticesVis.draw(gl, pvm, screen_size);
		segmentsVis.draw(gl, pvm);
		
		
	}
	
	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());	
		verticesVis.setColor(pointColor);
	}
	
	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		segmentsVis.setColor(lineColor);
		
	}
	
	@Override
	public void setPointSize(float point_size) {

		pointSize=point_size;
		verticesVis.setSize(pointSize);
	}
	

	@Override
	public void setLineThickness(float line_thickness) {


		lineThickness=line_thickness;
		segmentsVis.setThickness(lineThickness);
		updateRenderVertices();
	}
	
	
	@Override
	public void setRenderType(int nRenderType){
		
		
		renderType=nRenderType;
		segmentsVis.setRenderType(renderType);
		updateRenderVertices();

	}	

	
	/** Methods from imglib2 Polyline, 
	 * I do not really understand them yet, 
	 * but added for the future and implemented them
	 * to the best of my knowledge (and so they do not produce errors)
	 */
	
	public int numVertices() {
		return vertices.size();
	}
	
	
	public int numSegments() {
		return segments.size();
	}

	@Override
	public RealMaskRealInterval and(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.and(this, paramPredicate);
	}

	@Override
	public RealMaskRealInterval minus(Predicate<? super RealLocalizable> paramPredicate) {
		
		// TODO Auto-generated method stub
		return Masks.minus(this, paramPredicate);
	}

	@Override
	public RealMask negate() {
		// TODO Auto-generated method stub
		return Masks.negate(this);
	}

	@Override
	public RealMask or(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.or(this, paramPredicate);
	}

	@Override
	public RealMask xor(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.xor(this, paramPredicate);
	}

	@Override
	public boolean test(RealLocalizable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int numDimensions() {
		// TODO Auto-generated method stub
		return 3;
	}

	@Override
	public double realMin(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double realMax(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RealLocalizableRealPositionable vertex(int paramInt) {
		// TODO Auto-generated method stub
		return (RealLocalizableRealPositionable) vertices.get(paramInt);
	}

	@Override
	public void addVertex(int paramInt, RealLocalizable paramRealLocalizable) {
		// TODO Auto-generated method stub
		vertices.add((RealPoint) paramRealLocalizable);
	}

	@Override
	public void removeVertex(int paramInt) {
		// TODO Auto-generated method stub
		vertices.remove(paramInt);
	}

	@Override
	public void addVertices(int paramInt, Collection<RealLocalizable> paramCollection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveRoi(final FileWriter writer)
	{
		int i, iPoint, iSegment;
		float [] vert;
		ArrayList<RealPoint> segment;
		
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat df3 = new DecimalFormat ("#.###", symbols);
		try {
			writer.write("Type," + Roi3D.intTypeToString(this.getType())+"\n");
			writer.write("Name," + this.getName()+"\n");
			writer.write("GroupInd," + Integer.toString(this.getGroupInd())+"\n");
			writer.write("PointSize," + df3.format(this.getPointSize())+"\n");
			writer.write("PointColor,"+ Integer.toString(pointColor.getRed()) +","
									  +	Integer.toString(pointColor.getGreen()) +","
									  +	Integer.toString(pointColor.getBlue()) +","
									  +	Integer.toString(pointColor.getAlpha()) +"\n");
			writer.write("LineThickness," + df3.format(this.getLineThickness())+"\n");
			writer.write("LineColor,"+ Integer.toString(lineColor.getRed()) +","
									  +	Integer.toString(lineColor.getGreen()) +","
									  +	Integer.toString(lineColor.getBlue()) +","
									  +	Integer.toString(lineColor.getAlpha()) +"\n");
			writer.write("RenderType,"+ Integer.toString(this.getRenderType())+"\n");
			
			writer.write("Vertices,"+Integer.toString(vertices.size())+"\n");
			vert = new float[3];
			for (iPoint = 0;iPoint<vertices.size();iPoint++)
			{ 
				vertices.get(iPoint).localize(vert);
				for(i=0;i<3;i++)
				{
					writer.write(df3.format(vert[i])+",");
				}
				//time point
				writer.write("0.0\n");
			}
			writer.write("SegmentsNumber,"+Integer.toString(segments.size())+"\n");
			for(iSegment=0;iSegment<segments.size();iSegment++)
			{
				segment=segments.get(iSegment);
				writer.write("Segment,"+Integer.toString(iSegment+1)+",Points,"+Integer.toString(segment.size())+"\n");
				for (iPoint = 0;iPoint<segment.size();iPoint++)
				{ 
					segment.get(iPoint).localize(vert);
					for(i=0;i<3;i++)
					{
						writer.write(df3.format(vert[i])+",");
					}
					//time point
					writer.write("0.0\n");
				}
			}
		}
		catch (IOException e) {	
			System.err.print(e.getMessage());
			
		}
	}

	@Override
	public void reversePoints() {
		
		int i;
		
		vertices = Roi3D.reverseArrayRP(vertices); 
		//update drawing component
		verticesVis.setVertices(vertices);
		
		ArrayList<ArrayList<RealPoint>> segments_r = new ArrayList<ArrayList<RealPoint>>();
		for(i= segments.size()-1;i>=0;i--)
		{
			segments_r.add(Roi3D.reverseArrayRP(segments.get(i)));
		}
		segments = segments_r;
		
		//update drawing component
		updateRenderVertices();
		
		return;
		
	}
	@Override
	public void setGroup(final Roi3DGroup preset_in) {
		
		setPointColor(preset_in.pointColor);
		setLineColor(preset_in.lineColor);

		setRenderType(preset_in.renderType);
		setPointSize(preset_in.pointSize);
		setLineThickness(preset_in.lineThickness);
	}


	/** returns the length of LineTrace using globCal voxel size **/
	public double getLength(final int nShapeInterpolation, final double [] globCal)
	{
		//get measured length
		return  Roi3D.getSegmentLength(makeJointSegment(nShapeInterpolation),globCal);

		
	}
	public double getEndsDistance(final double [] globCal)
	{
		if(vertices.size()>1)
		{
			double [] posB = new double [3];
			double [] posE = new double [3];
			Roi3D.scaleGlob(vertices.get(0),globCal).localize(posB);
			Roi3D.scaleGlob(vertices.get(vertices.size()-1),globCal).localize(posE);
			return LinAlgHelpers.distance(posB, posE);
		}
		else
		{
			
			return Double.NaN;
		}
			
	}
	public void getEnds(final MeasureValues val, final double [] globCal)
	{
		val.ends = new RealPoint [2];
		val.ends[0]= new RealPoint(Roi3D.scaleGlob(vertices.get(0),globCal));
		if(vertices.size()>1)
		{
			val.ends[1]= new RealPoint(Roi3D.scaleGlob(vertices.get(vertices.size()-1),globCal));
		}
		else
		{
			val.ends[1] =Roi3D.getNaNPoint();
		}
		return;
	}

	/** returns direction of the vector from one to another end**/
	public void getEndsDirection(final MeasureValues val, final double [] globCal)
	{
		if(vertices.size()>1)
		{
			double [] posB = new double [3];
			double [] posE = new double [3];
			Roi3D.scaleGlob(vertices.get(0),globCal).localize(posB);
			Roi3D.scaleGlob(vertices.get(vertices.size()-1),globCal).localize(posE);
			LinAlgHelpers.subtract(posE, posB, posE);
			LinAlgHelpers.normalize(posE);
			val.direction=new RealPoint(posE);
		}
		else
		{
			
			val.direction = Roi3D.getNaNPoint();
		}
			
	}
	/** returns joint segment of ROI in VOXEL coordinates **/
	public ArrayList<RealPoint> makeJointSegment(final int nShapeInterpolation)
	{
		ArrayList<RealPoint> out = new ArrayList<RealPoint>();
		if(vertices.size()>1)
		{
			//first vertex
			out.add(vertices.get(0));
			for(int i=0;i<segments.size(); i++)
			{
				for(int j = 1; j<segments.get(i).size();j++)
				{
					out.add(segments.get(i).get(j));
				}
			}
			if(nShapeInterpolation == BigTraceData.SHAPE_Subvoxel)
			{
				out = ShapeInterpolation.getSmoothVals(out);
			}
		}
		else 
		{
			return null;
		}
		return out;
	}
	/**Creates a sampled set of points along the LineTrace in SPACE coordinates,
	 * based on the shape interpolation value.
	 * Segments are sampled with a smallest voxel size step.
	 * **/
	public ArrayList<RealPoint> makeJointSegmentResample(final int nShapeInterpolation, final double [] globCal)
	{
		ArrayList<RealPoint> out = makeJointSegment(nShapeInterpolation);
		
		return sampleMinVoxelSize(Roi3D.scaleGlob(out, globCal),nShapeInterpolation,globCal);
		//return out;
	}
	
	public ArrayList<RealPoint> sampleMinVoxelSize(final ArrayList<RealPoint> points, final int nShapeInterpolation, final double [] globCal)
	{
		double dMinVoxSize = Math.min(Math.min(globCal[0], globCal[1]),globCal[2]);
		switch (nShapeInterpolation)
		{
		case BigTraceData.SHAPE_Voxel:

			break;
			
		case BigTraceData.SHAPE_Subvoxel:					

			break;
		}
		//if we didn't do this interpolation before
		if(lastInterpolation!=nShapeInterpolation || linInter==null)
		{
			linInter = new LinInterp3D(points);
			lastInterpolation = nShapeInterpolation;
		}
		double dLength = linInter.getMaxLength();
		int nNewPoints =(int) Math.ceil(dLength/ dMinVoxSize);
		double [] xLSample = new double[nNewPoints];
		for(int i = 0;i<nNewPoints;i++)
		{
			xLSample[i]=i*dMinVoxSize;
		}
		return linInter.interpolate(xLSample);
	}
	/** returns double [i][j] array where for position i
	 * 0 is length along the line (in scaled units)
	 * 1 intensity
	 * 2 x coordinate (in scaled units) 
	 * 3 y coordinate (in scaled units) 
	 * 4 z coordinate (in scaled units) **/
	public < T extends RealType< T > >  double [][] getIntensityProfile(final IntervalView<T> source, final double [] globCal, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory, final int nShapeInterpolation)
	{
		ArrayList<RealPoint> allPoints = makeJointSegmentResample(nShapeInterpolation,globCal);
		
		if(allPoints==null)
			return null;
		//if(nShapeInterpolation == BigTraceData.SHAPE_Subvoxel)
		//{
		//	allPoints = ShapeInterpolation.getSmoothVals(allPoints);
		//}
		
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		
		return getIntensityProfilePoints(allPoints,interpolate,globCal);
	}
	/** returns cosine or an angle (from 0 to pi, determined by bCosine) 
	 *  between dir_vector (assumed to have length of 1.0) and each segment of the line Roi. 
	 *  The output is double [i][j] array where for position i
	 * 0 is length along the line (in scaled units)
	 * 1 orientation (cosine or angle in radians)
	 * 2 x coordinate (in scaled units) 
	 * 3 y coordinate (in scaled units) 
	 * 4 z coordinate (in scaled units) **/
	public double [][] getCoalignmentProfile(final double [] dir_vector, final double [] globCal, final int nShapeInterpolation, final boolean bCosine)
	{
		ArrayList<RealPoint> allPoints = makeJointSegmentResample(nShapeInterpolation,globCal);
		
		if(allPoints==null)
			return null;

		return getCoalignmentProfilePoints(allPoints, dir_vector, bCosine);
		//return getCoalignmentProfilePoints(allPoints, dir_vector, globCal,  bCosine);
	}
	
	@Override
	public void updateRenderVertices() {
		
		//update drawing component
		if(segments.size()>0)
		{
			segmentsVis.setVertices(makeJointSegment(BigTraceData.shapeInterpolation));
		}
		//reset interpolators
		lastInterpolation = -1;
		
	}
}

