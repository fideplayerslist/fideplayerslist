use strict;

my $MAX_CNT=1000000;
my $cnt=0;

my $reference_year=2015;

my @listed_fields=qw(name country sex birthday rating title);

my $command='';

while(!($command=~/x/i))
{
	print qq(
x = exit
p = preprocess
as = age stats
cs = country stats
fc = field counts
yt = young talents
ot = old talents
hm = high rated men ( >=2600 )
hw = high rated women ( >=2400 )
h = high rated players ( >=2500 )
ga = GM average ratings
sd = standard deviation
yp = young players

enter command: );
	$command=<>;
	chomp($command);
	
	print "\n";
	
	if($command=~/^p$/i)
	{
		$cnt=0;
		preprocess();
	}
	
	if($command=~/as/i)
	{
		age_stats();
	}
	
	if($command=~/cs/i)
	{
		country_stats();
	}
	
	if($command=~/fc/i)
	{
		field_counts();
	}
	
	if($command=~/yt/i)
	{
		young_talents(10,30,'young',50);
	}
	
	if($command=~/sd/i)
	{
		young_talents(1,100,'std_dev',50);
	}
	
	if($command=~/ot/i)
	{
		young_talents(60,100,'old',100);
	}
	
	if($command=~/hm/i)
	{
		high_rated_players("M",2600);
	}
	
	if($command=~/hw/i)
	{
		high_rated_players("F",2400);
	}
	
	if($command=~/h$/i)
	{
		high_rated_players("M|F",2500);
	}
	
	if($command=~/ga/i)
	{
		gm_average_ratings();
	}
	
	if($command=~/yp/i)
	{
		young_players();
	}
	
}

sub young_players
{

	my $young_txt='';

	iterate(sub {
		my $record=shift;
		
		if(($record->{age}>=4)&&($record->{age}<=6)&&($record->{rating}>0)&&($record->{sex}=~/M|F/))
		{
			
			$young_txt.="$record->{line}\n";
			
		}
		
	});
	
	print $young_txt;
	
	save("young_players.txt",$young_txt);

}



sub young_talents
{
	
	my ($from,$to,$name,$size)=@_;

	open(AGE_STATS,"age_stats.txt");
	
	my $age_stats={};
	
	while(<AGE_STATS>)
	{
		chomp $_;
		my @fields=split /\t/,$_;
		
		my $age=$fields[0];
		my $RM=$fields[1];
		my $AVGRM=$fields[2];
		my $RF=$fields[3];
		my $AVGRF=$fields[4];
		
		$age_stats->{$age}->{RM}=$RM;
		$age_stats->{$age}->{AVGRM}=$AVGRM;
		$age_stats->{$age}->{RF}=$RF;
		$age_stats->{$age}->{AVGRF}=$AVGRF;
	}
	
	close(AGE_STATS);
	
	my $list={};
	
	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/M|F/)
		&&($record->{age}>=$from)&&($record->{age}<=$to))
		{
			my $expected_rating=$age_stats->{$record->{age}}->{"AVGR$record->{sex}"};
			my $rating_surplus=$record->{rating}-$expected_rating;
			
			$list->{"$record->{name}\t$record->{country}\t$record->{sex}\t$record->{age}\t".int($expected_rating)."\t$record->{rating}"}=$rating_surplus;
			
			
			$age_stats->{$record->{age}}->{"DEVSQ$record->{sex}"}+=
			$rating_surplus*$rating_surplus;
			
			$age_stats->{TOT}->{"DEVSQ$record->{sex}"}+=
			$rating_surplus*$rating_surplus;
			
			$age_stats->{TOT}->{"R$record->{sex}"}++;
			
			
		}
	});
	
	my @talents=keys %{$list};
	
	@talents=sort
	{
		$list->{$b}<=>$list->{$a};
	}
	@talents;
	
	my $young_talents_txt='';
	
	for(my $i=0;$i<$size;$i++)
	{
		my $index=$i+1;
		my $item="$index. $talents[$i]\t".int($list->{$talents[$i]});
		
		$young_talents_txt.="$item\n";
	}
	
	print $young_talents_txt;
	
	save($name."_talents.txt",$young_talents_txt,"name\tcountry\tgender\tage\texpected rating\tactual rating\trating surplus");
	
	my $std_dev_txt='';
	
	my $TOTDEVSQM=$age_stats->{TOT}->{DEVSQM};
	my $TOTRM=$age_stats->{TOT}->{RM};
	my $TOTAVGDEVSQM=ratio($TOTDEVSQM,$TOTRM);
	my $TOTSTDDEVM=sprintf "%.1f",sqrt($TOTAVGDEVSQM);
	my $TOTDEVSQF=$age_stats->{TOT}->{DEVSQF};
	my $TOTRF=$age_stats->{TOT}->{RF};
	my $TOTAVGDEVSQF=ratio($TOTDEVSQF,$TOTRF);
	my $TOTSTDDEVF=sprintf "%.1f",sqrt($TOTAVGDEVSQF);
	
	for(my $age=$from;$age<=$to;$age++)
	{
	
		my $DEVSQM=$age_stats->{$age}->{DEVSQM};
		my $RM=$age_stats->{$age}->{RM};
		my $AVGDEVSQM=ratio($DEVSQM,$RM);
		my $STDDEVM=sprintf "%.1f",sqrt($AVGDEVSQM);
		my $DEVSQF=$age_stats->{$age}->{DEVSQF};
		my $RF=$age_stats->{$age}->{RF};
		my $AVGDEVSQF=ratio($DEVSQF,$RF);
		my $STDDEVF=sprintf "%.1f",sqrt($AVGDEVSQF);
		
		my $age_field=($age%5==0?$age:'');
		
		$std_dev_txt.="$age_field\t$STDDEVM\t$STDDEVF\n";
	
	}
	
	$std_dev_txt.="TOT\t$TOTSTDDEVM\t$TOTSTDDEVF\n";
	
	save("std_dev.txt",$std_dev_txt,"age\tstd dev male\tstd dev female");
	
}

sub ratio
{
	my ($cumulative,$count)=@_;
	
	if($count>0)
	{
		return $cumulative/$count;
	}
	
	return 'N/A';
}

sub averagef
{
	my ($cumulative,$count)=@_;
	
	if($count>0)
	{
		return sprintf "%.1f",$cumulative/$count;
	}
	
	return 'N/A';
}

sub percentf
{
	my ($ratio)=@_;
	
	if($ratio ne 'N/A')
	{
		return sprintf "%.2f",$ratio*100;
	}
	
	return 'N/A';
}

sub save
{
	my ($file_name,$content,$html_headers)=@_;
	
	open(OUTF,">$file_name");
	print OUTF $content;
	close(OUTF);
	
	if($html_headers ne '')
	{
	
		$content="$html_headers\n$content";
		
		my @content=split /\n/,$content;
		
		my $html_content="<table border=1 cellpadding=3 cellspacing=3>";
		
		foreach(@content)
		{
		
			my @fields=split /\t/,$_;
			
			my $item=join('',map { "<td align=center>$_</td>"; } @fields);
			
			$html_content.="<tr>$item</tr>\n";
		
		}
		
		$html_content.="</table>";
		
		$file_name=~s/.txt$/.html/;
		save($file_name,$html_content);
	
	}
}

sub age_stats
{
	
	my $age_stats={};
	
	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/M|F/)&&($record->{age} ne ''))
		{
			$age_stats->{$record->{age}}->{"R$record->{sex}"}++;
			$age_stats->{$record->{age}}->{"CR$record->{sex}"}+=$record->{rating};
			
			if($record->{title} eq 'GM')
			{
				$age_stats->{$record->{age}}->{"GM$record->{sex}"}++;
			}
		}
	});
	
	my $age_stats_txt='';
	
	for(my $i=1;$i<=100;$i++)
	{
	
		my $RM=$age_stats->{$i}->{RM};
		my $AVGRM=averagef($age_stats->{$i}->{CRM},$RM);
		my $GMM=$age_stats->{$i}->{GMM};
		my $RF=$age_stats->{$i}->{RF};
		my $AVGRF=averagef($age_stats->{$i}->{CRF},$RF);
		my $GMF=$age_stats->{$i}->{GMF};
		
		my $age_field=($i%5==0?$i:'');
		
		my $item="$age_field\t$RM\t$AVGRM\t$RF\t$AVGRF\t$GMM\t$GMF";
		
		$age_stats_txt.="$item\n";
	
	}
	
	print $age_stats_txt;
	
	save("age_stats.txt",$age_stats_txt,"age\trated males\taverage rating males\trated females\taverage rating females\tmale GMs\tfemale GMs");
	
}

sub country_stats
{
	
	my $country_stats={};
	
	iterate(sub {
		my $record=shift;
		
		if(($record->{sex}=~/M|F/)&&($record->{country} ne '')&&($record->{name} ne '')&&($record->{age} ne ''))
		{
			$country_stats->{$record->{country}}->{"$record->{sex}"}++;
			if($record->{rating}>0)
			{
				$country_stats->{$record->{country}}->{"R$record->{sex}"}++;
				$country_stats->{$record->{country}}->{"CR$record->{sex}"}+=$record->{rating};
			}
		}
	});
	
	my $country_stats_txt='';
	
	my @countries=keys %{$country_stats};
	
	my @filtered_countries=();
	
	foreach(@countries)
	{
	
		my $country=$_;
		
		$country_stats->{$country}->{TOT}=
		$country_stats->{$country}->{M}+$country_stats->{$country}->{F};
		
		$country_stats->{$country}->{RTOT}=
		$country_stats->{$country}->{RM}+$country_stats->{$country}->{RF};
		
		$country_stats->{$country}->{FRATIO}=ratio($country_stats->{$country}->{F},$country_stats->{$country}->{TOT});
		
		$country_stats->{$country}->{FRATIOR}=ratio($country_stats->{$country}->{RF},$country_stats->{$country}->{RTOT});
		
		$country_stats->{$country}->{FPERCENT}=percentf($country_stats->{$country}->{FRATIO});
		
		$country_stats->{$country}->{FPERCENTR}=percentf($country_stats->{$country}->{FRATIOR});
		
		$country_stats->{$country}->{AVGRM}=averagef($country_stats->{$country}->{CRM},$country_stats->{$country}->{RM});
		
		$country_stats->{$country}->{AVGRF}=averagef($country_stats->{$country}->{CRF},$country_stats->{$country}->{RF});
		
		$country_stats->{$country}->{AVGRDIFF}=
			(($country_stats->{$country}->{AVGRM} ne 'N/A')&&($country_stats->{$country}->{AVGRF} ne 'N/A'))?sprintf "%.1f",$country_stats->{$country}->{AVGRM}-$country_stats->{$country}->{AVGRF}:'N/A';
		
		if($country_stats->{$country}->{TOT}>1000)
		{
		
			push(@filtered_countries,$_);
		
		}
		
	}
	
	@filtered_countries=sort
	{
		$country_stats->{$b}->{FRATIO}<=>$country_stats->{$a}->{FRATIO};
	}
	@filtered_countries;
	
	foreach(@filtered_countries)
	{
	
		my $country=$_;
	
		my $TOT=$country_stats->{$country}->{TOT};
		my $M=$country_stats->{$country}->{M};
		my $RM=$country_stats->{$country}->{RM};
		my $AVGRM=$country_stats->{$country}->{AVGRM};
		my $F=$country_stats->{$country}->{F};
		my $RF=$country_stats->{$country}->{RF};
		my $AVGRF=$country_stats->{$country}->{AVGRF};
		my $FPERCENT=$country_stats->{$country}->{FPERCENT};
		my $FPERCENTR=$country_stats->{$country}->{FPERCENTR};
		my $AVGRDIFF=$country_stats->{$country}->{AVGRDIFF};
		
		my $item="$country\t$TOT\t$M\t$RM\t$AVGRM\t$F\t$RF\t$AVGRF\t$FPERCENT\t$FPERCENTR\t$AVGRDIFF";
		
		$country_stats_txt.="$item\n";
	
	}
	
	print $country_stats_txt;
	
	save("country_stats.txt",$country_stats_txt,"country\ttotal players\tmales\trated males\taverage rating males\tfemales\trated females\taverage rating females\tfemale % of all players\tfemale % of rated players\taverage rating difference");
	
}

sub field_counts
{

	my $field_counts={};
	
	iterate(sub {
		my $record=shift;
		
		foreach(@listed_fields)
		{
			my $key=$_;
			
			if($record->{$key} ne '')
			{
				$field_counts->{$key}++;
			}
		}
		
	});
	
	my $field_counts_txt='';
	
	foreach(@listed_fields)
		{
			my $key=$_;
			
			my $item="$key\t$field_counts->{$key}";
			
			$field_counts_txt.="$item\n";
		}
		
	print $field_counts_txt;
		
	save("field_counts.txt",$field_counts_txt,"field\tnon empty count");

}

sub high_rated_players
{

	my ($sex,$floor)=@_;

	my $birthdays={};
	
	my $items={};

	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>=$floor)&&($record->{sex}=~/$sex/)&&($record->{age} ne ''))
		{
			my $item="$record->{name} \t$record->{country}\t$record->{sex}\t$record->{birthday}\t$record->{rating}\t$record->{age}\t$record->{title}";
			
			$items->{$item}=$record->{rating};
		}
	});
	
	my @items=keys %{$items};
	
	@items=sort
	{
	
		$items->{$b}<=>$items->{$a};
	
	}
	@items;
	
	my $high_rated_txt='';
	
	for(my $i=0;$i<@items;$i++)
	{
	
		$high_rated_txt.=(($i+1).".\t$items[$i]\n");
	
	}
	
	print $high_rated_txt;
	
	save("high_rated_txt",$high_rated_txt,"rank\tname\tcountry\tgender\tbirthday\trating\tage\ttitle");

}

sub gm_average_ratings
{

	my $gm_avgr={};

	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/F|M/)
		&&($record->{age}>=20)&&($record->{age}<=40)
		&&($record->{title} eq 'GM'))
		{
			$gm_avgr->{"CR$record->{sex}"}+=$record->{rating};
			$gm_avgr->{"R$record->{sex}"}++;
		}
	});
	
	my $RM=$gm_avgr->{RM};
	my $GMAVGRM=averagef($gm_avgr->{CRM},$RM);
	my $RF=$gm_avgr->{RF};
	my $GMAVGRF=averagef($gm_avgr->{CRF},$RF);
	
	my $gm_average_ratings_txt="$RM\t$GMAVGRM\t$RF\t$GMAVGRF\n";
	
	print $gm_average_ratings_txt;
	
	save("gm_average_ratings.txt",$gm_average_ratings_txt,"male GMs\tmale GM average rating\tfemale GMs\tfemale GM average rating");

}

sub iterate
{

	my $sub=shift;

	open(PLAYERS,"players.txt");
	
	while(my $line=<PLAYERS>)
	{
	
		chomp($line);
		
		my $record={};
		
		my @fields=split /\t/,$line;
		
		foreach(@listed_fields)
		{
		
			my $key=$_;
			
			my $value=shift @fields;
			
			$record->{$key}=$value;
		
		}
		
		$record->{line}=$line;
		
		if($record->{birthday} ne '')
		{
			$record->{age}=$reference_year-$record->{birthday};
		}
		else
		{
			$record->{age}='';
		}
		
		&{$sub}($record);
	
	}
	
	close(PLAYERS);

}

sub preprocess
{

	print "preprocessing players\n\n";

	open(PLAYERS,"players_list_xml.xml");
	
	open(PLAYERS_TXT,">players.txt");
	
	my $chunk='';
	
	my $chars=0;
	
	my $field_counts={};
	
	while((my $line=<PLAYERS>)&&($cnt<$MAX_CNT))
	{
	
		#print $line;
	
		$chars+=length($line);
	
		chomp($line);
		
		$chunk.=$line;
		
		my @chunk=split /<player>/,$chunk;
		
		if(@chunk>1)
		{
		
			for(my $i=1;$i<@chunk;$i++)
			{
				
				if($chunk[$i]=~/<\/player>/)
				{
				
					my @record=split /<\/player>/,$chunk[$i];
					
					my $record=$record[0];
					
					$chunk=~s/<player>\Q$record<\/player>//;
					
					my $fields={};
					
					while($record=~/<([^>]+)>([^<]+)<\/[^>]+>/g)
					{
						
						my $key=$1;
						my $value=$2;
						
						$fields->{$key}=$value;
						
					}
					
					my @fields=();
					foreach(@listed_fields)
					{
					
						my $key=$_;
						
						my $value=$fields->{$key};
						
						push(@fields,$value);
						
						if($value ne '')
						{
							$field_counts->{$key}++;
						}
						
					}
					
					my $line_out=join("\t",@fields);
					
					print PLAYERS_TXT "$line_out\n";
				
					$cnt++;
					
					if(($cnt%10000)==0)
					{
					
						print "$cnt players processed\n\n";
						
						my @field_counts=();
						foreach(@listed_fields)
						{
						
							my $key=$_;
							
							print "has $key : $field_counts->{$key}\n";
							
						}
						
						print "\n";
					
					}
				
				}
			
			}
			
		}
	
	}
	
	close(PLAYERS_TXT);
	
	close(PLAYERS);
	
	print "preprocessing OK, $cnt players processed\n";

}