use strict;

use GD;

my $AXISEXCESS=10;

my $BOXSIZE=3;

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

graph_inactivity();
graph_hw();

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
	my ($x,$y,$color,$offset)=@_;
		
	$im->filledRectangle($x-$offset,$y-$offset,$x+$offset,$y+$offset,$color);
}

sub draw_axis
{

	$im->filledRectangle(0,0,$WIDTH+3*$MARGIN,$HEIGHT+3*$MARGIN,$white);

	my ($crossx,$crossy,$title,$xtitle,$ytitle,$legend,$xfreq,$yfreq)=@_;
	
	my $scrx=calc_scr($WIDTH,$MINX,$MAXX,$crossx);
	my $scry=calc_scr($HEIGHT,$MAXY,$MINY,$crossy);
	
	$im->rectangle($scrx,$MARGIN-$AXISEXCESS,$scrx,$MARGIN+$HEIGHT+$AXISEXCESS,$black);
	
	$im->rectangle($MARGIN-$AXISEXCESS,$scry,$MARGIN+$WIDTH+$AXISEXCESS,$scry,$black);
	
	$im->string(gdLargeFont,$MARGIN*1.5,$MARGIN/2,$title,$black);
	$im->string(gdLargeFont,$MARGIN+$WIDTH/2,$MARGIN+$HEIGHT+$MARGIN/2,$xtitle,$black);
	$im->stringUp(gdLargeFont,$MARGIN/2-15,$MARGIN+$HEIGHT/2+40,$ytitle,$black);
	
	for(my $x=$MINX;$x<=$MAXX;$x+=int(($MAXX-$MINX)/$xfreq))
	{
		my $scrxc=calc_scr($WIDTH,$MINX,$MAXX,$x);
		
		$im->string(gdLargeFont,$scrxc-4,$MARGIN+$HEIGHT+20,$x,$black);
		
		$im->rectangle($scrxc,$scry-3,$scrxc,$scry+3,$black);
	}
	
	for(my $y=$MINY;$y<=$MAXY;$y+=($MAXY-$MINY)/$yfreq)
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
		
		draw_box($X0,$Y0,$color,$BOXSIZE);
		
		$im->string(gdLargeFont,$X0+20,$Y0-8,$title,$black);
		
		$Y0+=40;
	}
}

sub save_image
{
	my ($name)=@_;
	
	open(OUT,">chart_$name.png");

    # make sure we are writing to a binary stream
    binmode OUT;

    # Convert the image to PNG and print it on standard output
    print OUT $im->png;
	
	close(OUT);
}

sub graph_hw
{

	my @hw=read_data("high_rated_F_above_2100");
	
	my @data=read_data("country_stats_1_100");
	
	my $pars={};
	
	foreach(@data)
	{
		my $record=$_;
		
		$pars->{$record->{country}}=$record->{'female % of rated players'};
	}
	
	$MAXX=@hw-1;
	$MINY=8;
	$MAXY=23;

    draw_axis(0,8,"Participation rate in top female players countries","rank","female participation %",[["participation",$red]],10,5);
	
	my $cnt=0;
	
	my $cpar=0;
	
	my $chunk=25;
	
	foreach(@hw)
	{
		my $record=$_;
		
		my $rank=$record->{rank};
		
		my $par=$pars->{$record->{country}};
		
		$cpar+=$par;
		
		$cnt++;
		
		if($cnt>=$chunk)
		{
		
			$par=$cpar/$chunk;
			
			$cpar=0;
		
			#print "$rank $record->{name} $par\n";
			
			my $x=calc_scr($WIDTH,$MINX,$MAXX,$rank);
			my $y=calc_scr($HEIGHT,$MAXY,$MINY,$par);
			
			draw_box($x,$y,$red,5);
			
			$cnt=0;
		
		}
	}
	
	save_image("high_ranked_participation");

}

sub graph_inactivity
{

	my @data=read_data("age_stats");

    draw_axis(0,0,"Inactivity rate of rated FIDE players in the function of age","age","inactivity %",[["male",$blue],["female",$red]],20,20);
	
	$MAXX=100;
	$MINY=0;
	$MAXY=100;
	
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
			draw_box($x,$yM,$blue,$BOXSIZE);
		}
		
		if(($iF>0)&&($iF<100))
		{
			draw_box($x,$yF,$red,$BOXSIZE);
		}
	}
	
	save_image("inactivity");

}