use strict;

use GD;

my $AXISEXCESS=10;

my $BOXSIZE=5;

my $WIDTH=500;
my $HEIGHT=350;

my $MINX=0;
my $MAXX=100;

my $MINY=0;
my $MAXY=100;

my $MARGIN=100;

# create a new image
my $im = new GD::Image($WIDTH+3*$MARGIN,$HEIGHT+2*$MARGIN);

# allocate some colors
my $white = $im->colorAllocate(255,255,255);
my $black = $im->colorAllocate(0,0,0);       
my $red = $im->colorAllocate(255,0,0);      
my $blue = $im->colorAllocate(0,0,255);

graph();

sub read_data
{

	my ($name)=@_;
	
	open(INF,"stats/$name.txt");
	
	my $fields=<INF>;
	
	chomp($fields);
	
	my @fields=split /\t/,$fields;
	
	my @data=();
	
	foreach(<INF>)
	{
		my $record={};
		
		my $line=$_;
		
		chomp($line);
		
		my @line=split /\t/,$line;
		
		for(my $i=0;$i<@line;$i++)
		{
			$record->{$fields[$i]}=$line[$i];
		}
		
		push(@data,$record);
	}
	
	close(INF);
	
	return @data;

}

sub calc_scr
{
	my ($SIZE,$MIN,$MAX,$x)=@_;
	
	return $SIZE*($x-$MIN)/($MAX-$MIN)+$MARGIN;
}

sub draw_box
{
	my ($x,$y,$color)=@_;
	
	for(my $j=0;$j<$BOXSIZE/2;$j++)
	{
		$im->rectangle($x-$j,$y-$j,$x+$j,$y+$j,$color);
	}
}

sub draw_axis
{
	my ($crossx,$crossy,$title,$xtitle,$ytitle,$legend)=@_;
	
	my $scrx=calc_scr($WIDTH,$MINX,$MAXX,$crossx);
	my $scry=calc_scr($HEIGHT,$MAXY,$MINY,$crossy);
	
	$im->rectangle($scrx,$MARGIN-$AXISEXCESS,$scrx,$MARGIN+$HEIGHT+$AXISEXCESS,$black);
	
	$im->rectangle($MARGIN-$AXISEXCESS,$scry,$MARGIN+$WIDTH+$AXISEXCESS,$scry,$black);
	
	$im->string(gdLargeFont,$MARGIN*1.5,$MARGIN/2,$title,$black);
	$im->string(gdLargeFont,$MARGIN+$WIDTH/2,$MARGIN+$HEIGHT+$MARGIN/2,$xtitle,$black);
	$im->stringUp(gdLargeFont,$MARGIN/2-15,$MARGIN+$HEIGHT/2+40,$ytitle,$black);
	
	for(my $x=$MINX;$x<=$MAXX;$x+=($MAXX-$MINX)/20)
	{
		my $scrxc=calc_scr($WIDTH,$MINX,$MAXX,$x);
		
		$im->string(gdLargeFont,$scrxc-4,$MARGIN+$HEIGHT+20,$x,$black);
		
		$im->rectangle($scrxc,$scry-3,$scrxc,$scry+3,$black);
	}
	
	for(my $y=$MINY;$y<=$MAXY;$y+=($MAXY-$MINY)/20)
	{
		my $scryc=calc_scr($HEIGHT,$MAXY,$MINY,$y);
		
		$im->string(gdLargeFont,$MARGIN-35,$scryc-8,$y,$black);
		
		$im->rectangle($scrx-3,$scryc,$scrx+3,$scryc,$black);
	}
	
	my $Y0=2*$MARGIN;
	
	foreach(@{$legend})
	{
		my $item=$_;
		
		my $title=$item->[0];
		my $color=$item->[1];
		
		my $X0=$MARGIN*1.5+$WIDTH;
		
		draw_box($X0,$Y0,$color);
		
		$im->string(gdLargeFont,$X0+20,$Y0-8,$title,$black);
		
		$Y0+=40;
	}
}

sub graph
{

	my @data=read_data("age_stats");

    draw_axis(0,0,"Inactivity rate of rated FIDE players in the function of age","age","inactivity %",[["male",$blue],["female",$red]]);
	
	foreach(@data)
	{
		my $record=$_;
		
		my $age=$record->{age};
		
		my $iM=$record->{'rated inactive males %'};
		my $iF=$record->{'rated inactive females %'};
		
		my $x=calc_scr($WIDTH,$MINX,$MAXX,$age);
		my $yM=calc_scr($HEIGHT,$MAXY,$MINY,$iM);
		my $yF=calc_scr($HEIGHT,$MAXY,$MINY,$iF);
		
		if(($iM>0)&&($iM<100))
		{
			draw_box($x,$yM,$blue);
		}
		
		if(($iF>0)&&($iF<100))
		{
			draw_box($x,$yF,$red);
		}
	}

    # make the background transparent and interlaced
    #$im->transparent($white);
    #$im->interlaced('true');

    # Put a black frame around the picture
    #$im->rectangle(0,0,99,99,$black);

    # Draw a blue oval
    #$im->arc(50,50,95,75,0,360,$blue);

    # And fill it with red
    #$im->fill(50,50,$red);
	
	#$im->string(gdSmallFont,2,10,"Peachy Keen",$blue);
	
	open(OUT,">chart.png");

    # make sure we are writing to a binary stream
    binmode OUT;

    # Convert the image to PNG and print it on standard output
    print OUT $im->png;
	
	close(OUT);

}