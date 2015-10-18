use strict;

my $MAX_CNT=1000000;
my $cnt=0;

my $reference_year=2015;

my @listed_fields=qw(name country sex birthday rating);

my $command='';

while(!($command=~/x/i))
{
	print "\nx = exit\np = preprocess\nas = age stats\nyt = yount talents\nhw = high rated women\n\nenter command: ";
	$command=<>;
	chomp($command);
	
	print "\n";
	
	if($command=~/p/i)
	{
		$cnt=0;
		preprocess();
	}
	
	if($command=~/as/i)
	{
		age_stats();
	}
	
	if($command=~/yt/i)
	{
		young_talents();
	}
	
	if($command=~/hw/i)
	{
		high_rated_women();
	}
	
}

sub young_talents
{
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
		
		$age_stats->{$age}->{AVGRM}=$AVGRM;
		$age_stats->{$age}->{AVGRF}=$AVGRF;
	}
	
	close(AGE_STATS);
	
	my $list={};
	
	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>0)&&($record->{sex}=~/M|F/)
		&&($record->{age}>0)&&($record->{age}<=30))
		{
			my $expected_rating=$age_stats->{$record->{age}}->{"AVGR$record->{sex}"};
			my $rating_surplus=$record->{rating}-$expected_rating;
			
			$list->{"$record->{name}\t$record->{country}\t$record->{sex}\t$record->{age}\t".int($expected_rating)."\t$record->{rating}"}=$rating_surplus;
		}
	});
	
	my @talents=keys %{$list};
	
	@talents=sort
	{
		$list->{$b}<=>$list->{$a};
	}
	@talents;
	
	my $young_talents_txt='';
	
	for(my $i=0;$i<50;$i++)
	{
		my $index=$i+1;
		my $item="$index. $talents[$i] ".int($list->{$talents[$i]});
		
		$young_talents_txt.="$item\n";
	}
	
	print $young_talents_txt;
	
	save("young_talents.txt",$young_talents_txt);
	
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

sub save
{
	my ($file_name,$content)=@_;
	
	open(OUTF,">$file_name");
	print OUTF $content;
	close(OUTF);
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
		}
	});
	
	my $age_stats_txt='';
	
	for(my $i=1;$i<=100;$i++)
	{
	
		my $RM=$age_stats->{$i}->{RM};
		my $AVGRM=averagef($age_stats->{$i}->{CRM},$RM);
		my $RF=$age_stats->{$i}->{RF};
		my $AVGRF=averagef($age_stats->{$i}->{CRF},$RF);
		
		my $item="$i\t$RM\t$AVGRM\t$RF\t$AVGRF";
		
		$age_stats_txt.="$item\n";
	
	}
	
	print $age_stats_txt;
	
	save("age_stats.txt",$age_stats_txt);
	
}

sub high_rated_women
{

	my $birthdays={};
	
	my $items={};

	iterate(sub {
		my $record=shift;
		
		if(($record->{rating}>2400)&&($record->{sex} eq 'F'))
		{
			my $item=sprintf "%-40s %3s %1s %4d %8d %4d",$record->{name},$record->{country},$record->{sex},$record->{birthday},$record->{rating},$record->{age};
			
			$items->{$item}=$record->{rating};
		}
	});
	
	my @items=keys %{$items};
	
	@items=sort
	{
	
		$items->{$b}<=>$items->{$a};
	
	}
	@items;
	
	print join("\n",@items),"\n";

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